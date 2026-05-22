package domains.submission.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.utils.ProblemCommandSupport.canManageProblem
import domains.problem.table.ProblemTable
import domains.submission.application.input.{CreateSubmissionRequest}
import domains.submission.model.{SubmissionId, SubmissionJudgeState, SubmissionStatus}
import domains.submission.table.SubmissionTable
import domains.submission.application.SubmissionCommandResults.*
import domains.submission.application.utils.SubmissionCommandSupport.*

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
        ProblemTable.findBySlug(connection, validRequest.problemSlug).flatMap {
          case None =>
            IO.pure(CreateSubmissionResult.ProblemNotFound)
          case Some(problem) =>
            canSubmitToProblem(connection, actor, problem).flatMap {
              case false =>
                IO.pure(CreateSubmissionResult.Forbidden)
              case true =>
                for
                  created <- SubmissionTable.insert(
                    connection = connection,
                    problemId = problem.id,
                    problemSlug = problem.slug,
                    problemTitle = problem.title,
                    submitterUsername = actor.username,
                    language = validRequest.language,
                    sourceCode = validRequest.sourceCode,
                  )
                  manageable <- canManageProblem(connection, actor, problem)
                yield CreateSubmissionResult.Created(created.copy(canManage = manageable))
            }
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
        ProblemTable.findBySlug(connection, submission.problemSlug).flatMap {
          case None =>
            IO.pure(DeleteSubmissionResult.NotFound)
          case Some(problem) =>
            canManageProblem(connection, actor, problem).flatMap {
              case false =>
                IO.pure(DeleteSubmissionResult.Forbidden)
              case true =>
                SubmissionTable.deleteById(connection, submissionId).as(DeleteSubmissionResult.Deleted)
            }
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
        ProblemTable.findBySlug(connection, submission.problemSlug).flatMap {
          case None =>
            IO.pure(RejudgeSubmissionResult.NotFound)
          case Some(problem) =>
            canManageProblem(connection, actor, problem).flatMap {
              case false =>
                IO.pure(RejudgeSubmissionResult.Forbidden)
              case true if submission.status == SubmissionStatus.Queued || submission.status == SubmissionStatus.Running =>
                IO.pure(RejudgeSubmissionResult.ValidationFailed("Only completed or failed submissions can be rejudged."))
              case true =>
                SubmissionTable
                  .updateJudgeState(connection, submissionId, SubmissionJudgeState.queued)
                  .flatMap(_ => SubmissionTable.findById(connection, submissionId))
                  .map(_.getOrElse(throw new IllegalStateException("Submission disappeared after rejudge.")))
                  .map(_.copy(canManage = true))
                  .map(RejudgeSubmissionResult.Rejudged(_))
            }
        }
    }
