package domains.problemset.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.table.ProblemTable
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSet, ProblemSetSummaryView, UpdateProblemSetRequest}
import domains.problemset.table.ProblemSetTable
import domains.shared.model.{PageRequest, PageResponse}

object ProblemSetCommands:

  enum CreateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case Created(problemSet: ProblemSet)

  enum AddProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemAlreadyLinked
    case Linked(problemSet: ProblemSet)

  enum GetProblemSetResult:
    case NotFound
    case Found(problemSet: ProblemSet)

  enum UpdateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case Updated(problemSet: ProblemSet)

  enum DeleteProblemSetResult:
    case Forbidden
    case ProblemSetNotFound
    case Deleted

  enum RemoveProblemResult:
    case Forbidden
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemNotLinked
    case Removed(problemSet: ProblemSet)

  def listProblemSets(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSetSummaryView]] =
    val _ = actor
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.list(connection, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def createProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemSetRequest
  ): IO[CreateProblemSetResult] =
    if !ProblemSetPolicy.canCreate(actor) then
      IO.pure(CreateProblemSetResult.Forbidden)
    else
      ProblemSetValidation.validateCreate(request) match
        case Left(message) =>
          IO.pure(CreateProblemSetResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              existing <- ProblemSetTable.findBySlug(connection, validRequest.slug)
              result <- existing match
                case Some(_) =>
                  IO.pure(CreateProblemSetResult.SlugAlreadyExists)
                case None =>
                  ProblemSetTable
                    .insert(connection, actor.username, validRequest)
                    .map(problemSet => CreateProblemSetResult.Created(problemSet))
            yield result
          }

  def getProblemSetBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problemset.model.ProblemSetSlug
  ): IO[GetProblemSetResult] =
    val _ = actor
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.findBySlug(connection, slug).map {
        case Some(problemSet) => GetProblemSetResult.Found(problemSet)
        case None => GetProblemSetResult.NotFound
      }
    }

  def addProblemToProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: AddProblemToProblemSetRequest
  ): IO[AddProblemResult] =
    if !ProblemSetPolicy.canManageProblems(actor) then
      IO.pure(AddProblemResult.Forbidden)
    else
      ProblemSetValidation.validateAddProblem(request) match
        case Left(message) =>
          IO.pure(AddProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
              result <- maybeProblemSet match
                case None =>
                  IO.pure(AddProblemResult.ProblemSetNotFound)
                case Some(problemSet) =>
                  for
                    maybeProblem <- ProblemTable.findBySlug(connection, validRequest.problemSlug)
                    linkedResult <- maybeProblem match
                      case None =>
                        IO.pure(AddProblemResult.ProblemNotFound)
                      case Some(problem) =>
                        ProblemSetTable
                          .addProblem(connection, problemSet.id, problem.id)
                          .flatMap {
                            case ProblemSetTable.AddProblemTableResult.AlreadyLinked =>
                              IO.pure(AddProblemResult.ProblemAlreadyLinked)
                            case ProblemSetTable.AddProblemTableResult.Linked =>
                              ProblemSetTable
                                .findBySlug(connection, problemSet.slug)
                                .map {
                                  case Some(updatedProblemSet) => AddProblemResult.Linked(updatedProblemSet)
                                  case None => throw new IllegalStateException("Problem set disappeared after problem link")
                                }
                          }
                  yield linkedResult
            yield result
          }

  def updateProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: UpdateProblemSetRequest
  ): IO[UpdateProblemSetResult] =
    if !ProblemSetPolicy.canEdit(actor) then
      IO.pure(UpdateProblemSetResult.Forbidden)
    else
      ProblemSetValidation.validateUpdate(request) match
        case Left(message) =>
          IO.pure(UpdateProblemSetResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
              result <- maybeProblemSet match
                case None =>
                  IO.pure(UpdateProblemSetResult.ProblemSetNotFound)
                case Some(problemSet) =>
                  ProblemSetTable
                    .update(connection, problemSet.id, validRequest)
                    .flatMap(_ =>
                      ProblemSetTable.findBySlug(connection, problemSet.slug).map {
                        case Some(updatedProblemSet) => UpdateProblemSetResult.Updated(updatedProblemSet)
                        case None => throw new IllegalStateException("Problem set disappeared after update")
                      }
                    )
            yield result
          }

  def deleteProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug
  ): IO[DeleteProblemSetResult] =
    if !ProblemSetPolicy.canDelete(actor) then
      IO.pure(DeleteProblemSetResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
          result <- maybeProblemSet match
            case None =>
              IO.pure(DeleteProblemSetResult.ProblemSetNotFound)
            case Some(problemSet) =>
              ProblemSetTable.delete(connection, problemSet.id).as(DeleteProblemSetResult.Deleted)
        yield result
      }

  def removeProblemFromProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[RemoveProblemResult] =
    if !ProblemSetPolicy.canManageProblems(actor) then
      IO.pure(RemoveProblemResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
          result <- maybeProblemSet match
            case None =>
              IO.pure(RemoveProblemResult.ProblemSetNotFound)
            case Some(problemSet) =>
              for
                maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
                removeResult <- maybeProblem match
                  case None =>
                    IO.pure(RemoveProblemResult.ProblemNotFound)
                  case Some(problem) =>
                    ProblemSetTable.removeProblem(connection, problemSet.id, problem.id).flatMap {
                      case ProblemSetTable.RemoveProblemTableResult.NotLinked =>
                        IO.pure(RemoveProblemResult.ProblemNotLinked)
                      case ProblemSetTable.RemoveProblemTableResult.Removed =>
                        ProblemSetTable.findBySlug(connection, problemSet.slug).map {
                          case Some(updatedProblemSet) => RemoveProblemResult.Removed(updatedProblemSet)
                          case None => throw new IllegalStateException("Problem set disappeared after problem removal")
                        }
                    }
              yield removeResult
        yield result
      }
