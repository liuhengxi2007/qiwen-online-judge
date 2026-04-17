package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.submission.application.SubmissionCommands
import domains.submission.model.{CreateSubmissionRequest, SubmissionId}

import java.sql.Connection

object SubmissionHttpPlans:

  case object ListSubmissions extends PlainSubmissionHttpPlan[Option[Username], SubmissionCommands.ListSubmissionsResult]:

    override val name: String = "ListSubmissions"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: Option[Username]
    ): IO[SubmissionCommands.ListSubmissionsResult] =
      SubmissionCommands.listSubmissions(databaseSession, actor, input)

  case object CreateSubmission extends TransactionSubmissionHttpPlan[CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult]:

    override val name: String = "CreateSubmission"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateSubmissionRequest
    ): IO[SubmissionCommands.CreateSubmissionResult] =
      SubmissionCommands.createSubmission(connection, actor, input)

  case object GetSubmission extends PlainSubmissionHttpPlan[SubmissionId, SubmissionCommands.GetSubmissionResult]:

    override val name: String = "GetSubmission"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: SubmissionId
    ): IO[SubmissionCommands.GetSubmissionResult] =
      SubmissionCommands.getSubmission(databaseSession, actor, input)
