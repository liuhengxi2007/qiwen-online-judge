package domains.problem.application

import cats.effect.IO
import database.{DatabaseSession, ResourceAccessGrantTable}
import domains.auth.model.AuthUser
import domains.problem.model.{CreateProblemRequest, ProblemId, UpdateProblemRequest}
import domains.problem.table.ProblemTable
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.ProblemCommandSupport.*
import domains.problem.application.ProblemDecisions.*
import domains.shared.access.{ResourceId, ResourceKind}

import java.time.Instant
import java.util.UUID

object ProblemMutationCommands:

  def createProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemRequest
  ): IO[CreateProblemResult] =
    databaseSession.withTransactionConnection(connection =>
      createProblem(connection, actor, request)
    )

  def createProblem(
    connection: java.sql.Connection,
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
          for
            existing <- ProblemTable.findBySlug(connection, validRequest.slug)
            conflictingProblemSet <- findConflictingProblemSet(connection, validRequest.slug.value)
            accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
            result <- decideCreateProblem(existing, conflictingProblemSet, accessPolicyValidation) match
              case CreateProblemDecision.SlugAlreadyExists =>
                IO.pure(CreateProblemResult.SlugAlreadyExists)
              case CreateProblemDecision.SlugConflictsWithProblemSet =>
                IO.pure(CreateProblemResult.SlugConflictsWithProblemSet)
              case CreateProblemDecision.ValidationFailed(message) =>
                IO.pure(CreateProblemResult.ValidationFailed(message))
              case CreateProblemDecision.Create =>
                for
                  problemId <- IO.delay(ProblemId(UUID.randomUUID()))
                  now <- IO.realTimeInstant
                  result <- ProblemTable
                    .insert(connection, problemId, now, actor.username, sanitizePolicy(validRequest))
                    .map(problem => CreateProblemResult.Created(problem.copy(canManage = true)))
                yield result
          yield result

  def updateProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemRequest
  ): IO[UpdateProblemResult] =
    databaseSession.withTransactionConnection(connection =>
      updateProblem(connection, actor, problemSlug, request)
    )

  def updateProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemRequest
  ): IO[UpdateProblemResult] =
    ProblemValidation.validateUpdate(request) match
      case Left(message) =>
        IO.pure(UpdateProblemResult.ValidationFailed(message))
      case Right(validRequest) =>
        for
          maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
          result <- maybeProblem match
            case None =>
              IO.pure(UpdateProblemResult.ProblemNotFound)
            case Some(problem) =>
              canManageProblem(connection, actor, problem).flatMap {
                case false =>
                  IO.pure(UpdateProblemResult.Forbidden)
                case true =>
                  validateAccessPolicySubjects(connection, validRequest.accessPolicy).flatMap {
                    case Some(message) =>
                      IO.pure(UpdateProblemResult.ValidationFailed(message))
                    case None =>
                      ProblemTable
                        .update(connection, problem.id, Instant.now(), sanitizePolicy(validRequest))
                        .flatMap(_ =>
                          ProblemTable
                            .findBySlug(connection, problem.slug)
                            .map(updatedProblemOrError("Problem disappeared after update"))
                            .map(_.copy(canManage = true))
                            .map(UpdateProblemResult.Updated(_))
                        )
                  }
              }
        yield result

  def deleteProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[DeleteProblemResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteProblem(connection, actor, problemSlug)
    )

  def deleteProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[DeleteProblemResult] =
    for
      maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
      result <- maybeProblem match
        case None =>
          IO.pure(DeleteProblemResult.ProblemNotFound)
        case Some(problem) =>
          canManageProblem(connection, actor, problem).flatMap {
            case false =>
              IO.pure(DeleteProblemResult.Forbidden)
            case true =>
              ResourceAccessGrantTable
                .deleteAllForResource(connection, ResourceKind.Problem, ResourceId(problem.id.value))
                .flatMap(_ => ProblemTable.delete(connection, problem.id))
                .as(DeleteProblemResult.Deleted)
          }
    yield result
