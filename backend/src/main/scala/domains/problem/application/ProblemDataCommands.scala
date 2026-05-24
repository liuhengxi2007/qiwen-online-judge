package domains.problem.application



import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.judge.application.JudgeCommands
import domains.problem.application.output.{ProblemDataFileListResponse, ProblemDataTreeResponse, ProblemDataUploadResult, ProblemDetail}
import domains.problem.model.{ProblemDataFilename, ProblemDataManifestEntry, ProblemDataPath, ProblemDataTreeNode, ProblemDataTreeNodeKind, ProblemSlug}
import domains.problem.table.problem.ProblemTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.utils.ProblemCommandSupport.*

import java.time.Instant

object ProblemDataCommands:
  import ProblemDataStorage.*

  private def sha256Hex(bytes: Array[Byte]): String =
    java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def writePreparedFiles(
    problemDataStorage: ProblemDataStorage,
    problemSlug: domains.problem.model.ProblemSlug,
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): IO[Unit] =
    preparedFiles.traverse_ { preparedFile =>
      IO.fromEither(
        ProblemDataUploadPreparation
          .toProblemDataPath(preparedFile.path)
          .leftMap(message => IllegalArgumentException(message))
      ).flatMap(path => problemDataStorage.writePath(problemSlug, path, preparedFile.bytes).void)
    }

  private def summaryFilenameFor(
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): Either[String, ProblemDataFilename] =
    preparedFiles
      .sortBy(_.path.value)
      .lastOption
      .toRight("Uploaded archive does not contain any files.")
      .flatMap(file => ProblemDataFilename.parse(file.path.fileName))

  private def withManageableProblemForUpdate[A](
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug
  )(notFound: A, forbidden: A)(useProblem: ProblemDetail => IO[A]): IO[A] =
    ProblemTable.findBySlugForUpdate(connection, problemSlug).flatMap {
      case None =>
        IO.pure(notFound)
      case Some(problem) =>
        canManageProblem(connection, actor, problem).ifM(useProblem(problem), IO.pure(forbidden))
    }

  def uploadProblemDataFile(
    problemDataStorage: ProblemDataStorage,
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
            persistPreparedUpload(problemDataStorage, connection, actor, problemSlug, List(preparedFile), summaryFilename)

  def uploadProblemDataArchive(
    problemDataStorage: ProblemDataStorage,
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
            persistPreparedUpload(problemDataStorage, connection, actor, problemSlug, preparedFiles, summaryFilename)

  private def persistPreparedUpload(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    preparedFiles: List[shared.application.upload.PreparedUploadFile],
    summaryFilename: ProblemDataFilename
  ): IO[UpdateProblemDataResult] =
    withManageableProblemForUpdate(connection, actor, problemSlug)(
      UpdateProblemDataResult.ProblemNotFound,
      UpdateProblemDataResult.Forbidden
    ) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        now = Instant.now()
        result <- writePreparedFiles(problemDataStorage, problem.slug, preparedFiles)
          .flatMap(_ => ProblemTable.updateData(connection, problem.id, now, summaryFilename))
          .flatMap(_ => ProblemDataFileTable.upsertForProblem(connection, problem.id, toManifestEntries(preparedFiles), now))
          .flatMap(_ =>
            ProblemTable
              .findBySlug(connection, problem.slug)
              .map(updatedProblemOrError("Problem disappeared after data update"))
              .map(_.copy(canManage = true))
              .map(problem => ProblemDataUploadResult(problem, preparedFiles.length))
              .map(UpdateProblemDataResult.Updated(_))
          )
          .handleErrorWith { error =>
            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
          }
      yield result
    }

  def listProblemData(
    problemDataStorage: ProblemDataStorage,
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
              problemDataStorage
                .listFiles(problem.slug)
                .map(files => ListProblemDataResult.Listed(ProblemDataFileListResponse(files)))
          }
      }
    }

  def authorizeProblemDataDownload(
    problemDataStorage: ProblemDataStorage,
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[AuthorizeProblemDataDownloadResult] =
    val _ = problemDataStorage
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
    problemDataStorage: ProblemDataStorage,
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    filename: ProblemDataFilename
  ): IO[DeleteProblemDataResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteProblemData(problemDataStorage, connection, actor, problemSlug, filename)
    )

  def deleteProblemData(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    filename: ProblemDataFilename
  ): IO[DeleteProblemDataResult] =
    withManageableProblemForUpdate(connection, actor, problemSlug)(
      DeleteProblemDataResult.ProblemNotFound,
      DeleteProblemDataResult.Forbidden
    ) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        result <- problemDataStorage.deleteFile(problem.slug, filename).flatMap {
          case false =>
            IO.pure(DeleteProblemDataResult.DataFileNotFound)
          case true =>
            ProblemDataFileTable.deleteForProblemPath(connection, problem.id, ProblemDataPath.fromFilename(filename))
              .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
              .flatMap(entries => ProblemTable.updateData(connection, problem.id, Instant.now(), summaryFilenameForEntries(entries)))
              .flatMap(_ =>
                ProblemTable
                  .findBySlug(connection, problem.slug)
                  .map(updatedProblemOrError("Problem disappeared after data deletion"))
                  .map(_.copy(canManage = true))
                  .map(DeleteProblemDataResult.Deleted(_))
              )
              .handleErrorWith { error =>
                problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
              }
        }
      yield result
    }

  def deleteProblemDataPath(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    path: ProblemDataPath
  ): IO[DeleteProblemDataResult] =
    withManageableProblemForUpdate(connection, actor, problemSlug)(
      DeleteProblemDataResult.ProblemNotFound,
      DeleteProblemDataResult.Forbidden
    ) { problem =>
      for
        entries <- ProblemDataFileTable.listForProblem(connection, problem.id)
        pathsToDelete = entries.map(_.path).filter(entryPath => entryPath == path || entryPath.value.startsWith(s"${path.value}/"))
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        result <-
          if pathsToDelete.isEmpty then
            IO.pure(DeleteProblemDataResult.DataFileNotFound)
          else
            pathsToDelete
              .traverse_(pathToDelete => problemDataStorage.deletePath(problem.slug, pathToDelete).void)
              .flatMap(_ => pathsToDelete.traverse_(pathToDelete => ProblemDataFileTable.deleteForProblemPath(connection, problem.id, pathToDelete)))
              .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
              .flatMap(entries => ProblemTable.updateData(connection, problem.id, Instant.now(), summaryFilenameForEntries(entries)))
              .flatMap(_ =>
                ProblemTable
                  .findBySlug(connection, problem.slug)
                  .map(updatedProblemOrError("Problem disappeared after data deletion"))
                  .map(_.copy(canManage = true))
                  .map(DeleteProblemDataResult.Deleted(_))
              )
              .handleErrorWith { error =>
                problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
              }
      yield result
    }

  def clearProblemData(
    problemDataStorage: ProblemDataStorage,
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    databaseSession.withTransactionConnection(connection =>
      clearProblemData(problemDataStorage, connection, actor, problemSlug)
    )

  def clearProblemData(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    withManageableProblemForUpdate(connection, actor, problemSlug)(
      ClearProblemDataResult.ProblemNotFound,
      ClearProblemDataResult.Forbidden
    ) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        result <- problemDataStorage
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
            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
          }
      yield result
    }

  def setProblemDataReady(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    ready: Boolean
  ): IO[SetProblemReadyResult] =
    withManageableProblemForUpdate(connection, actor, problemSlug)(
      SetProblemReadyResult.ProblemNotFound,
      SetProblemReadyResult.Forbidden
    ) { problem =>
      if ready then markProblemReady(problemDataStorage, connection, problem)
      else markProblemNotReady(connection, problem)
    }

  private def markProblemNotReady(connection: java.sql.Connection, problem: ProblemDetail): IO[SetProblemReadyResult] =
    ProblemTable
      .updateDataReady(connection, problem.id, Instant.now(), problem.data.value, ready = false)
      .flatMap(_ => refreshedManagedProblem(connection, problem, "Problem disappeared after ready update"))
      .map(SetProblemReadyResult.Updated(_))

  private def markProblemReady(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    problem: ProblemDetail
  ): IO[SetProblemReadyResult] =
    val judgeYamlPath = ProblemDataPath("judge.yaml")
    for
      manifest <- ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug)
      maybeConfig <- problemDataStorage.readPath(problem.slug, judgeYamlPath)
      result <- maybeConfig match
        case None =>
          IO.pure(SetProblemReadyResult.ValidationFailed("judge.yaml is required at the problem data root."))
        case Some((_, bytes)) =>
          JudgeCommands
            .validateProblemReadyConfig(bytes, problem, manifest)
            .fold(
              message => IO.pure(SetProblemReadyResult.ValidationFailed(message)),
              retainedPaths => retainReadyFiles(problemDataStorage, connection, problem, manifest.entries, retainedPaths)
            )
    yield result

  private def retainReadyFiles(
    problemDataStorage: ProblemDataStorage,
    connection: java.sql.Connection,
    problem: ProblemDetail,
    entries: List[ProblemDataManifestEntry],
    retainedPaths: Set[ProblemDataPath]
  ): IO[SetProblemReadyResult] =
    val retainedEntries = entries.filter(entry => retainedPaths.contains(entry.path))
    val redundantPaths = entries.map(_.path).filterNot(retainedPaths.contains)
    for
      snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
      result <- redundantPaths
        .traverse_(path => problemDataStorage.deletePath(problem.slug, path).void)
        .flatMap(_ => ProblemDataFileTable.deleteExceptPaths(connection, problem.id, retainedPaths))
        .flatMap(_ => ProblemTable.updateDataReady(connection, problem.id, Instant.now(), summaryFilenameForEntries(retainedEntries), ready = true))
        .flatMap(_ => refreshedManagedProblem(connection, problem, "Problem disappeared after ready update"))
        .map(SetProblemReadyResult.Updated(_))
        .handleErrorWith { error =>
          problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
        }
    yield result

  private def refreshedManagedProblem(
    connection: java.sql.Connection,
    problem: ProblemDetail,
    missingMessage: String
  ): IO[ProblemDetail] =
    ProblemTable
      .findBySlug(connection, problem.slug)
      .map(updatedProblemOrError(missingMessage))
      .map(_.copy(canManage = true))

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

  private def toManifestEntries(preparedFiles: List[shared.application.upload.PreparedUploadFile]): List[ProblemDataManifestEntry] =
    preparedFiles.flatMap { preparedFile =>
      ProblemDataUploadPreparation.toProblemDataPath(preparedFile.path).toOption.map { path =>
        ProblemDataManifestEntry(path = path, sizeBytes = preparedFile.bytes.length.toLong, sha256 = sha256Hex(preparedFile.bytes))
      }
    }

  private def summaryFilenameForEntries(entries: List[ProblemDataManifestEntry]): Option[ProblemDataFilename] =
    entries
      .sortBy(_.path.value)
      .lastOption
      .flatMap(entry => ProblemDataFilename.parse(entry.path.fileName).toOption)
