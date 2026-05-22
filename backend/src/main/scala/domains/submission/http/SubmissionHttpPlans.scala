package domains.submission.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.submission.application.SubmissionCommands
import domains.submission.application.input.{CreateSubmissionRequest, SubmissionListRequest}
import domains.submission.model.{SubmissionId}

import java.sql.Connection

object SubmissionHttpPlans:

  case object ListSubmissions extends PlainAuthenticatedHttpPlan[SubmissionListRequest, SubmissionCommands.ListSubmissionsResult]:

    override val name: String = "ListSubmissions"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: SubmissionListRequest
    ): IO[SubmissionCommands.ListSubmissionsResult] =
      SubmissionCommands.listSubmissions(databaseSession, actor, input)

  case object CreateSubmission extends TransactionAuthenticatedHttpPlan[CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult]:

    override val name: String = "CreateSubmission"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateSubmissionRequest
    ): IO[SubmissionCommands.CreateSubmissionResult] =
      SubmissionCommands.createSubmission(connection, actor, input)

  case object GetSubmission extends PlainAuthenticatedHttpPlan[SubmissionId, SubmissionCommands.GetSubmissionResult]:

    override val name: String = "GetSubmission"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.GetSubmissionResult] =
      SubmissionCommands.getSubmission(databaseSession, actor, input)

  case object DeleteSubmission extends TransactionAuthenticatedHttpPlan[SubmissionId, SubmissionCommands.DeleteSubmissionResult]:

    override val name: String = "DeleteSubmission"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.DeleteSubmissionResult] =
      SubmissionCommands.deleteSubmission(connection, actor, input)

  case object RejudgeSubmission extends TransactionAuthenticatedHttpPlan[SubmissionId, SubmissionCommands.RejudgeSubmissionResult]:

    override val name: String = "RejudgeSubmission"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.RejudgeSubmissionResult] =
      SubmissionCommands.rejudgeSubmission(connection, actor, input)
