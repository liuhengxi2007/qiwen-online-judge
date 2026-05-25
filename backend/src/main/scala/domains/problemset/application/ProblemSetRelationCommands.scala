package domains.problemset.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.problemset.model.request.AddProblemToProblemSetRequest
import domains.problemset.table.problem_set.ProblemSetTable
import domains.problemset.application.ProblemSetCommandResults.*
import domains.problemset.application.ProblemSetDecisions.*
import domains.problemset.application.utils.ProblemSetCommandSupport.*

import java.sql.Connection

object ProblemSetRelationCommands:

  def addProblemToProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: AddProblemToProblemSetRequest
  ): IO[AddProblemResult] =
    databaseSession.withTransactionConnection(connection =>
      addProblemToProblemSet(connection, actor, problemSetSlug, request)
    )

  def addProblemToProblemSet(
    connection: Connection,
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
          for
            maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
            maybeProblem <- maybeProblemSet match
              case None => IO.pure(None)
              case Some(_) => ProblemCommands.resolveProblemSetMemberTarget(connection, validRequest.problemSlug)
            result <- decideAddProblem(maybeProblemSet, maybeProblem) match
              case AddProblemDecision.ProblemSetNotFound =>
                IO.pure(AddProblemResult.ProblemSetNotFound)
              case AddProblemDecision.ProblemNotFound =>
                IO.pure(AddProblemResult.ProblemNotFound)
              case AddProblemDecision.Link(problemSet, problem) =>
                ProblemSetTable
                  .addProblem(connection, problemSet.id, problem.id)
                  .flatMap {
                    case ProblemSetTable.AddProblemTableResult.AlreadyLinked =>
                      IO.pure(AddProblemResult.ProblemAlreadyLinked)
                    case ProblemSetTable.AddProblemTableResult.Linked =>
                      ProblemSetTable
                        .findBySlug(connection, problemSet.slug)
                        .map(updatedProblemSetOrError("Problem set disappeared after problem link"))
                        .map(AddProblemResult.Linked(_))
                  }
          yield result

  def removeProblemFromProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[RemoveProblemResult] =
    databaseSession.withTransactionConnection(connection =>
      removeProblemFromProblemSet(connection, actor, problemSetSlug, problemSlug)
    )

  def removeProblemFromProblemSet(
    connection: Connection,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[RemoveProblemResult] =
    if !ProblemSetPolicy.canManageProblems(actor) then
      IO.pure(RemoveProblemResult.Forbidden)
    else
      for
        maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
        maybeProblem <- maybeProblemSet match
          case None => IO.pure(None)
          case Some(_) => ProblemCommands.resolveProblemSetMemberTarget(connection, problemSlug)
        result <- decideRemoveProblem(maybeProblemSet, maybeProblem) match
          case RemoveProblemDecision.ProblemSetNotFound =>
            IO.pure(RemoveProblemResult.ProblemSetNotFound)
          case RemoveProblemDecision.ProblemNotFound =>
            IO.pure(RemoveProblemResult.ProblemNotFound)
          case RemoveProblemDecision.Remove(problemSet, problem) =>
            ProblemSetTable.removeProblem(connection, problemSet.id, problem.id).flatMap {
              case ProblemSetTable.RemoveProblemTableResult.NotLinked =>
                IO.pure(RemoveProblemResult.ProblemNotLinked)
              case ProblemSetTable.RemoveProblemTableResult.Removed =>
                ProblemSetTable
                  .findBySlug(connection, problemSet.slug)
                  .map(updatedProblemSetOrError("Problem set disappeared after problem removal"))
                  .map(RemoveProblemResult.Removed(_))
            }
      yield result
