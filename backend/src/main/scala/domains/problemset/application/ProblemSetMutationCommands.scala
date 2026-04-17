package domains.problemset.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.ProblemSlug
import domains.problem.table.ProblemTable
import domains.problemset.model.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.table.ProblemSetTable
import domains.shared.access.{ResourceAccessGrantTable, ResourceId, ResourceKind}
import domains.problemset.application.ProblemSetCommandResults.*
import domains.problemset.application.ProblemSetCommandSupport.*
import domains.problemset.application.ProblemSetDecisions.*

import java.sql.Connection

object ProblemSetMutationCommands:

  def createProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemSetRequest
  ): IO[CreateProblemSetResult] =
    databaseSession.withTransactionConnection(connection =>
      createProblemSet(connection, actor, request)
    )

  def createProblemSet(
    connection: Connection,
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
          for
            existing <- ProblemSetTable.findBySlug(connection, validRequest.slug)
            conflictingProblem <- ProblemSlug.parse(validRequest.slug.value) match
              case Left(message) => IO.raiseError(IllegalStateException(s"Validated problem set slug became invalid: $message"))
              case Right(problemSlug) => ProblemTable.findBySlug(connection, problemSlug)
            accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
            result <- decideCreateProblemSet(existing, conflictingProblem, accessPolicyValidation) match
              case CreateProblemSetDecision.SlugAlreadyExists =>
                IO.pure(CreateProblemSetResult.SlugAlreadyExists)
              case CreateProblemSetDecision.SlugConflictsWithProblem =>
                IO.pure(CreateProblemSetResult.SlugConflictsWithProblem)
              case CreateProblemSetDecision.ValidationFailed(message) =>
                IO.pure(CreateProblemSetResult.ValidationFailed(message))
              case CreateProblemSetDecision.Create =>
                ProblemSetTable
                  .insert(connection, actor.username, sanitizePolicy(validRequest))
                  .map(problemSet => CreateProblemSetResult.Created(problemSet))
          yield result

  def updateProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: UpdateProblemSetRequest
  ): IO[UpdateProblemSetResult] =
    databaseSession.withTransactionConnection(connection =>
      updateProblemSet(connection, actor, problemSetSlug, request)
    )

  def updateProblemSet(
    connection: Connection,
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
          for
            maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
            accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
            result <- decideUpdateProblemSet(maybeProblemSet, accessPolicyValidation) match
              case UpdateProblemSetDecision.ProblemSetNotFound =>
                IO.pure(UpdateProblemSetResult.ProblemSetNotFound)
              case UpdateProblemSetDecision.ValidationFailed(message) =>
                IO.pure(UpdateProblemSetResult.ValidationFailed(message))
              case UpdateProblemSetDecision.Update(problemSet) =>
                ProblemSetTable
                  .update(connection, problemSet.id, sanitizePolicy(validRequest))
                  .flatMap(_ =>
                    ProblemSetTable
                      .findBySlug(connection, problemSet.slug)
                      .map(updatedProblemSetOrError("Problem set disappeared after update"))
                      .map(UpdateProblemSetResult.Updated(_))
                  )
          yield result

  def deleteProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug
  ): IO[DeleteProblemSetResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteProblemSet(connection, actor, problemSetSlug)
    )

  def deleteProblemSet(
    connection: Connection,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug
  ): IO[DeleteProblemSetResult] =
    if !ProblemSetPolicy.canDelete(actor) then
      IO.pure(DeleteProblemSetResult.Forbidden)
    else
      for
        maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
        result <- maybeProblemSet match
          case None =>
            IO.pure(DeleteProblemSetResult.ProblemSetNotFound)
          case Some(problemSet) =>
            ResourceAccessGrantTable
              .deleteAllForResource(connection, ResourceKind.ProblemSet, ResourceId(problemSet.id.value))
              .flatMap(_ => ProblemSetTable.delete(connection, problemSet.id))
              .as(DeleteProblemSetResult.Deleted)
      yield result
