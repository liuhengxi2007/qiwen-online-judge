package domains.submission.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.submission.model.request.{CreateSubmissionRequest}
import domains.submission.model.{SubmissionId, SubmissionJudgeState, SubmissionStatus}
import domains.submission.table.submission.SubmissionTable
import domains.submission.application.SubmissionCommandResults.*

import java.sql.Connection

object SubmissionMutationCommands:

  def createSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateSubmissionRequest
  ): IO[CreateSubmissionResult] =
    databaseSession.withTransactionConnection(connection =>
      createSubmission(connection, actor, request)
    )

  def createSubmission(
    connection: Connection,
    actor: AuthUser,
    request: CreateSubmissionRequest
  ): IO[CreateSubmissionResult] =
    SubmissionValidation.validateCreate(request) match
      case Left(message) =>
        IO.pure(CreateSubmissionResult.ValidationFailed(message))
      case Right(validRequest) =>
        ProblemCommands.resolveSubmissionTarget(connection, actor, validRequest.problemSlug).flatMap {
          case ProblemCommands.ResolveSubmissionTargetResult.ProblemNotFound =>
            IO.pure(CreateSubmissionResult.ProblemNotFound)
          case ProblemCommands.ResolveSubmissionTargetResult.Forbidden =>
            IO.pure(CreateSubmissionResult.Forbidden)
          case ProblemCommands.ResolveSubmissionTargetResult.Resolved(problem) =>
            SubmissionTable
              .insert(
                connection = connection,
                problemId = problem.id,
                problemSlug = problem.slug,
                problemTitle = problem.title,
                submitterUsername = actor.username,
                language = validRequest.language,
                sourceCode = validRequest.sourceCode,
              )
              .map(created => CreateSubmissionResult.Created(created.copy(canManage = problem.canManage)))
        }

  def deleteSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[DeleteSubmissionResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteSubmission(connection, actor, submissionId)
    )

  def deleteSubmission(
    connection: Connection,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[DeleteSubmissionResult] =
    SubmissionTable.findById(connection, submissionId).flatMap {
      case None =>
        IO.pure(DeleteSubmissionResult.NotFound)
      case Some(submission) =>
        ProblemCommands.evaluateProblemAccess(connection, actor, submission.problemSlug).flatMap {
          case ProblemCommands.EvaluateProblemAccessResult.ProblemNotFound =>
            IO.pure(DeleteSubmissionResult.NotFound)
          case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access) if !access.canManage =>
            IO.pure(DeleteSubmissionResult.Forbidden)
          case ProblemCommands.EvaluateProblemAccessResult.Evaluated(_) =>
            SubmissionTable.deleteById(connection, submissionId).as(DeleteSubmissionResult.Deleted)
        }
    }

  def rejudgeSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[RejudgeSubmissionResult] =
    databaseSession.withTransactionConnection(connection =>
      rejudgeSubmission(connection, actor, submissionId)
    )

  def rejudgeSubmission(
    connection: Connection,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[RejudgeSubmissionResult] =
    SubmissionTable.findById(connection, submissionId).flatMap {
      case None =>
        IO.pure(RejudgeSubmissionResult.NotFound)
      case Some(submission) =>
        ProblemCommands.evaluateProblemAccess(connection, actor, submission.problemSlug).flatMap {
          case ProblemCommands.EvaluateProblemAccessResult.ProblemNotFound =>
            IO.pure(RejudgeSubmissionResult.NotFound)
          case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access) if !access.canManage =>
            IO.pure(RejudgeSubmissionResult.Forbidden)
          case ProblemCommands.EvaluateProblemAccessResult.Evaluated(_) if submission.status == SubmissionStatus.Queued || submission.status == SubmissionStatus.Running =>
            IO.pure(RejudgeSubmissionResult.ValidationFailed("Only completed or failed submissions can be rejudged."))
          case ProblemCommands.EvaluateProblemAccessResult.Evaluated(_) =>
            SubmissionTable
              .updateJudgeState(connection, submissionId, SubmissionJudgeState.queued)
              .flatMap(_ => SubmissionTable.findById(connection, submissionId))
              .map(_.getOrElse(throw new IllegalStateException("Submission disappeared after rejudge.")))
              .map(_.copy(canManage = true))
              .map(RejudgeSubmissionResult.Rejudged(_))
        }
    }
