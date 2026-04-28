package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{ProblemDataFileListResponse, ProblemDataFilename, UpdateProblemDataRequest}
import domains.problem.table.ProblemTable
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.ProblemCommandSupport.*

import java.time.Instant

object ProblemDataCommands:

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

  def updateProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemDataRequest
  ): IO[UpdateProblemDataResult] =
    databaseSession.withTransactionConnection(connection =>
      updateProblemData(connection, actor, problemSlug, request)
    )

  def updateProblemData(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemDataRequest
  ): IO[UpdateProblemDataResult] =
    ProblemValidation.validateDataUpdate(request) match
      case Left(message) =>
        IO.pure(UpdateProblemDataResult.ValidationFailed(message))
      case Right(validRequest) =>
        validRequest.decodedBytes match
          case Left(message) =>
            IO.pure(UpdateProblemDataResult.ValidationFailed(message))
          case Right(decodedBytes) =>
            ProblemDataUploadPreparation.prepareLegacyUpload(validRequest.filename, decodedBytes) match
              case Left(message) =>
                IO.pure(UpdateProblemDataResult.ValidationFailed(message))
              case Right(preparedFiles) =>
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
                              .flatMap(_ => ProblemTable.updateData(connection, problem.id, Instant.now(), validRequest.filename))
                              .flatMap(_ =>
                                ProblemTable
                                  .findBySlug(connection, problem.slug)
                                  .map(updatedProblemOrError("Problem disappeared after data update"))
                                  .map(_.copy(canManage = true))
                                  .map(UpdateProblemDataResult.Updated(_))
                              )
                              .handleErrorWith { error =>
                                ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                              }
                          yield result
                      }
                yield result

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
                  .flatMap(_ =>
                    ProblemTable
                      .findBySlug(connection, problem.slug)
                      .map(updatedProblemOrError("Problem disappeared after data update"))
                      .map(_.copy(canManage = true))
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
                  ProblemDataStorage
                    .listFiles(problem.slug)
                    .flatMap(files => ProblemTable.updateData(connection, problem.id, Instant.now(), files.lastOption))
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
