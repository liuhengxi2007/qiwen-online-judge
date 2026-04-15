package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{ProblemDataFileListResponse, ProblemDataFilename, UpdateProblemDataRequest}
import domains.problem.table.ProblemTable
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.ProblemCommandSupport.*

object ProblemDataCommands:

  def updateProblemData(
    databaseSession: DatabaseSession,
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
            databaseSession.withTransactionConnection { connection =>
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
                          result <- ProblemDataStorage
                            .writeFile(problem.slug, validRequest.filename, decodedBytes)
                            .flatMap(savedFilename =>
                              ProblemTable.updateData(connection, problem.id, savedFilename)
                                .flatMap(_ =>
                                  ProblemTable
                                    .findBySlug(connection, problem.slug)
                                    .map(updatedProblemOrError("Problem disappeared after data update"))
                                    .map(_.copy(canManage = true))
                                    .map(UpdateProblemDataResult.Updated(_))
                                )
                            )
                            .handleErrorWith { error =>
                              ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                            }
                        yield result
                    }
              yield result
            }

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
    databaseSession.withTransactionConnection { connection =>
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
                      .flatMap(files => ProblemTable.updateData(connection, problem.id, files.lastOption))
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
    }

  def clearProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    databaseSession.withTransactionConnection { connection =>
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
                  .flatMap(_ => ProblemTable.updateData(connection, problem.id, None))
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
    }
