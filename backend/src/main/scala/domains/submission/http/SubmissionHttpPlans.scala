package domains.submission.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.submission.application.SubmissionCommands
import domains.submission.model.request.{CreateSubmissionRequest, SubmissionListRequest}
import domains.submission.model.{SubmissionId}

import java.sql.Connection

object SubmissionHttpPlans:

  case object ListSubmissions extends PlainAuthenticatedHttpPlan[AuthUser, SubmissionListRequest, SubmissionCommands.ListSubmissionsResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: SubmissionListRequest
    ): IO[SubmissionCommands.ListSubmissionsResult] =
      SubmissionCommands.listSubmissions(databaseSession, actor, input)

  case object CreateSubmission extends TransactionAuthenticatedHttpPlan[AuthUser, CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateSubmissionRequest
    ): IO[SubmissionCommands.CreateSubmissionResult] =
      SubmissionCommands.createSubmission(connection, actor, input)

  case object GetSubmission extends PlainAuthenticatedHttpPlan[AuthUser, SubmissionId, SubmissionCommands.GetSubmissionResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.GetSubmissionResult] =
      SubmissionCommands.getSubmission(databaseSession, actor, input)

  case object DeleteSubmission extends TransactionAuthenticatedHttpPlan[AuthUser, SubmissionId, SubmissionCommands.DeleteSubmissionResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.DeleteSubmissionResult] =
      SubmissionCommands.deleteSubmission(connection, actor, input)

  case object RejudgeSubmission extends TransactionAuthenticatedHttpPlan[AuthUser, SubmissionId, SubmissionCommands.RejudgeSubmissionResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.RejudgeSubmissionResult] =
      SubmissionCommands.rejudgeSubmission(connection, actor, input)
