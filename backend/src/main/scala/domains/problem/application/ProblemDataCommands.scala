package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{ProblemDataFileListResponse, ProblemDataFilename, ProblemDataPath, ProblemDataTreeNode, ProblemDataTreeNodeKind, ProblemDataTreeResponse, ProblemDataUploadResult}
import domains.problem.table.{ProblemDataFileTable, ProblemTable}
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.ProblemCommandSupport.*

import java.time.Instant

object ProblemDataCommands:

  private def sha256Hex(bytes: Array[Byte]): String =
    java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def writePreparedFiles(
    problemSlug: domains.problem.model.ProblemSlug,
    preparedFiles: List[domains.shared.upload.PreparedUploadFile]
  ): IO[Unit] =
    preparedFiles.foldLeft(IO.unit) { (accIo, preparedFile) =>
      accIo *> (ProblemDataUploadPreparation.toProblemDataPath(preparedFile.path) match
        case Left(message) =>
          IO.raiseError(IllegalArgumentException(message))
        case Right(path) =>
          ProblemDataStorage.writePath(problemSlug, path, preparedFile.bytes).void)
    }

  private def summaryFilenameFor(
    preparedFiles: List[domains.shared.upload.PreparedUploadFile]
  ): Either[String, ProblemDataFilename] =
    preparedFiles
      .sortBy(_.path.value)
      .lastOption
      .toRight("Uploaded archive does not contain any files.")
      .flatMap(file => ProblemDataFilename.parse(file.path.fileName))

  def uploadProblemDataFile(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    path: domains.problem.model.ProblemDataPath,
    bytes: Array[Byte]
  ): IO[UpdateProblemDataResult] =
    ProblemDataUploadPreparation.prepareSingleFile(path, bytes) match
      case Left(message) =>
        IO.pure(UpdateProblemDataResult.ValidationFailed(message))
      case Right(preparedFile) =>
        summaryFilenameFor(List(preparedFile)) match
          case Left(message) =>
            IO.pure(UpdateProblemDataResult.ValidationFailed(message))
          case Right(summaryFilename) =>
            persistPreparedUpload(connection, actor, problemSlug, List(preparedFile), summaryFilename)

  def uploadProblemDataArchive(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    targetDirectory: Option[domains.problem.model.ProblemDataPath],
    archiveBytes: Array[Byte]
  ): IO[UpdateProblemDataResult] =
    ProblemDataUploadPreparation.prepareArchive(archiveBytes, targetDirectory) match
      case Left(message) =>
        IO.pure(UpdateProblemDataResult.ValidationFailed(message))
      case Right(preparedFiles) =>
        summaryFilenameFor(preparedFiles) match
          case Left(message) =>
            IO.pure(UpdateProblemDataResult.ValidationFailed(message))
          case Right(summaryFilename) =>
            persistPreparedUpload(connection, actor, problemSlug, preparedFiles, summaryFilename)

  private def persistPreparedUpload(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    preparedFiles: List[domains.shared.upload.PreparedUploadFile],
    summaryFilename: ProblemDataFilename
  ): IO[UpdateProblemDataResult] =
    for
      maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
      result <- maybeProblem match
        case None =>
          IO.pure(UpdateProblemDataResult.ProblemNotFound)
        case Some(problem) =>
          canManageProblem(connection, actor, problem).flatMap {
            case false =>
              IO.pure(UpdateProblemDataResult.Forbidden)
            case true =>
              for
                snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
                result <- writePreparedFiles(problem.slug, preparedFiles)
                  .flatMap(_ => ProblemTable.updateData(connection, problem.id, Instant.now(), summaryFilename))
                  .flatMap(_ => ProblemDataFileTable.replaceForProblem(connection, problem.id, toManifestEntries(preparedFiles), Instant.now()))
                  .flatMap(_ =>
                    ProblemTable
                      .findBySlug(connection, problem.slug)
                      .map(updatedProblemOrError("Problem disappeared after data update"))
                      .map(_.copy(canManage = true))
                      .map(problem => ProblemDataUploadResult(problem, preparedFiles.length))
                      .map(UpdateProblemDataResult.Updated(_))
                  )
                  .handleErrorWith { error =>
                    ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                  }
              yield result
          }
    yield result

  def listProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ListProblemDataResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, problemSlug).flatMap {
        case None =>
          IO.pure(ListProblemDataResult.ProblemNotFound)
        case Some(problem) =>
          canManageProblem(connection, actor, problem).flatMap {
            case false =>
              IO.pure(ListProblemDataResult.Forbidden)
            case true =>
              ProblemDataStorage
                .listFiles(problem.slug)
                .map(files => ListProblemDataResult.Listed(ProblemDataFileListResponse(files)))
          }
      }
    }

  def authorizeProblemDataDownload(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[AuthorizeProblemDataDownloadResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, problemSlug).flatMap {
        case None =>
          IO.pure(AuthorizeProblemDataDownloadResult.ProblemNotFound)
        case Some(problem) =>
          canManageProblem(connection, actor, problem).map {
            case true => AuthorizeProblemDataDownloadResult.Authorized
            case false => AuthorizeProblemDataDownloadResult.Forbidden
          }
      }
    }

  def listProblemDataTree(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ListProblemDataTreeResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, problemSlug).flatMap {
        case None =>
          IO.pure(ListProblemDataTreeResult.ProblemNotFound)
        case Some(problem) =>
          canManageProblem(connection, actor, problem).flatMap {
            case false =>
              IO.pure(ListProblemDataTreeResult.Forbidden)
            case true =>
              ProblemDataFileTable
                .manifestForProblem(connection, problem.id, problem.slug)
                .map(manifest => ListProblemDataTreeResult.Listed(buildTreeResponse(manifest.entries)))
          }
      }
    }

  def deleteProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    filename: ProblemDataFilename
  ): IO[DeleteProblemDataResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteProblemData(connection, actor, problemSlug, filename)
    )

  def deleteProblemData(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    filename: ProblemDataFilename
  ): IO[DeleteProblemDataResult] =
    ProblemTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        IO.pure(DeleteProblemDataResult.ProblemNotFound)
      case Some(problem) =>
        canManageProblem(connection, actor, problem).flatMap {
          case false =>
            IO.pure(DeleteProblemDataResult.Forbidden)
          case true =>
            for
              snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
              result <- ProblemDataStorage.deleteFile(problem.slug, filename).flatMap {
                case false =>
                  IO.pure(DeleteProblemDataResult.DataFileNotFound)
                case true =>
                  ProblemDataFileTable.deleteForProblemPath(connection, problem.id, ProblemDataPath.fromFilename(filename))
                    .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
                    .flatMap(entries => ProblemTable.updateData(connection, problem.id, Instant.now(), entries.lastOption.flatMap(entry => ProblemDataFilename.parse(entry.path.fileName).toOption)))
                    .flatMap(_ =>
                      ProblemTable
                        .findBySlug(connection, problem.slug)
                        .map(updatedProblemOrError("Problem disappeared after data deletion"))
                        .map(_.copy(canManage = true))
                        .map(DeleteProblemDataResult.Deleted(_))
                    )
                    .handleErrorWith { error =>
                      ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                    }
              }
            yield result
        }
    }

  def deleteProblemDataPath(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    path: ProblemDataPath
  ): IO[DeleteProblemDataResult] =
    ProblemTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        IO.pure(DeleteProblemDataResult.ProblemNotFound)
      case Some(problem) =>
        canManageProblem(connection, actor, problem).flatMap {
          case false =>
            IO.pure(DeleteProblemDataResult.Forbidden)
          case true =>
            for
              snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
              result <- ProblemDataStorage.deletePath(problem.slug, path).flatMap {
                case false =>
                  IO.pure(DeleteProblemDataResult.DataFileNotFound)
                case true =>
                  ProblemDataFileTable.deleteForProblemPath(connection, problem.id, path)
                    .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
                    .flatMap(entries => ProblemTable.updateData(connection, problem.id, Instant.now(), entries.lastOption.flatMap(entry => ProblemDataFilename.parse(entry.path.fileName).toOption)))
                    .flatMap(_ =>
                      ProblemTable
                        .findBySlug(connection, problem.slug)
                        .map(updatedProblemOrError("Problem disappeared after data deletion"))
                        .map(_.copy(canManage = true))
                        .map(DeleteProblemDataResult.Deleted(_))
                    )
                    .handleErrorWith { error =>
                      ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                    }
              }
            yield result
        }
    }

  def clearProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    databaseSession.withTransactionConnection(connection =>
      clearProblemData(connection, actor, problemSlug)
    )

  def clearProblemData(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    ProblemTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        IO.pure(ClearProblemDataResult.ProblemNotFound)
      case Some(problem) =>
        canManageProblem(connection, actor, problem).flatMap {
          case false =>
            IO.pure(ClearProblemDataResult.Forbidden)
          case true =>
            for
              snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
              result <- ProblemDataStorage
                .deleteAllFiles(problem.slug)
                .flatMap(_ => ProblemDataFileTable.deleteAllForProblem(connection, problem.id))
                .flatMap(_ => ProblemTable.updateData(connection, problem.id, Instant.now(), None))
                .flatMap(_ =>
                  ProblemTable
                    .findBySlug(connection, problem.slug)
                    .map(updatedProblemOrError("Problem disappeared after clearing data"))
                    .map(_.copy(canManage = true))
                    .map(ClearProblemDataResult.Cleared(_))
                )
                .handleErrorWith { error =>
                  ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                }
            yield result
        }
    }

  private def buildTreeResponse(entries: List[ProblemDataManifestEntry]): ProblemDataTreeResponse =
    val directoryNodes =
      entries
        .flatMap(entry => directoryPrefixes(entry.path))
        .distinct
        .sortBy(_.value)
        .map(path => ProblemDataTreeNode(path = path, kind = ProblemDataTreeNodeKind.Directory, sizeBytes = None))

    val fileNodes =
      entries
        .sortBy(_.path.value)
        .map(entry => ProblemDataTreeNode(path = entry.path, kind = ProblemDataTreeNodeKind.File, sizeBytes = Some(entry.sizeBytes)))

    ProblemDataTreeResponse(items = directoryNodes ++ fileNodes)

  private def directoryPrefixes(path: ProblemDataPath): List[ProblemDataPath] =
    val segments = path.value.split('/').toList.dropRight(1)
    segments.indices.map(index => ProblemDataPath(segments.take(index + 1).mkString("/"))).toList

  private def toManifestEntries(preparedFiles: List[domains.shared.upload.PreparedUploadFile]): List[ProblemDataManifestEntry] =
    preparedFiles.flatMap { preparedFile =>
      ProblemDataUploadPreparation.toProblemDataPath(preparedFile.path).toOption.map { path =>
        ProblemDataManifestEntry(path = path, sizeBytes = preparedFile.bytes.length.toLong, sha256 = sha256Hex(preparedFile.bytes))
      }
    }
