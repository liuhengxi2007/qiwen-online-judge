package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{CreateProblemRequest, ProblemDetail, ProblemListResponse, UpdateProblemRequest}
import domains.problem.table.ProblemTable
import domains.shared.model.PageRequest

object ProblemCommands:

  enum CreateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case Created(problem: ProblemDetail)

  enum GetProblemResult:
    case NotFound
    case Found(problem: ProblemDetail)

  enum UpdateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: ProblemDetail)

  enum DeleteProblemResult:
    case Forbidden
    case ProblemNotFound
    case Deleted

  def listProblems(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[ProblemListResponse] =
    val _ = actor
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.list(connection, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def createProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemRequest
  ): IO[CreateProblemResult] =
    if !ProblemPolicy.canCreate(actor) then
      IO.pure(CreateProblemResult.Forbidden)
    else
      ProblemValidation.validateCreate(request) match
        case Left(message) =>
          IO.pure(CreateProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              existing <- ProblemTable.findBySlug(connection, validRequest.slug)
              result <- existing match
                case Some(_) =>
                  IO.pure(CreateProblemResult.SlugAlreadyExists)
                case None =>
                  ProblemTable
                    .insert(connection, actor.username, validRequest)
                    .map(problem => CreateProblemResult.Created(problem))
            yield result
          }

  def getProblemBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problem.model.ProblemSlug
  ): IO[GetProblemResult] =
    val _ = actor
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, slug).map {
        case Some(problem) => GetProblemResult.Found(problem)
        case None => GetProblemResult.NotFound
      }
    }

  def updateProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemRequest
  ): IO[UpdateProblemResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(UpdateProblemResult.Forbidden)
    else
      ProblemValidation.validateUpdate(request) match
        case Left(message) =>
          IO.pure(UpdateProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
              result <- maybeProblem match
                case None =>
                  IO.pure(UpdateProblemResult.ProblemNotFound)
                case Some(problem) =>
                  ProblemTable
                    .update(connection, problem.id, validRequest)
                    .flatMap(_ =>
                      ProblemTable.findBySlug(connection, problem.slug).map {
                        case Some(updatedProblem) => UpdateProblemResult.Updated(updatedProblem)
                        case None => throw new IllegalStateException("Problem disappeared after update")
                      }
                    )
            yield result
          }

  def deleteProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[DeleteProblemResult] =
    if !ProblemPolicy.canDelete(actor) then
      IO.pure(DeleteProblemResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
          result <- maybeProblem match
            case None =>
              IO.pure(DeleteProblemResult.ProblemNotFound)
            case Some(problem) =>
              ProblemTable.delete(connection, problem.id).as(DeleteProblemResult.Deleted)
        yield result
      }
