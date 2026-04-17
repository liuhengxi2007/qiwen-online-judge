package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser

import java.sql.Connection

trait SubmissionHttpPlan[Input, Output]:

  def name: String

trait PlainSubmissionHttpPlan[Input, Output] extends SubmissionHttpPlan[Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait TransactionSubmissionHttpPlan[Input, Output] extends SubmissionHttpPlan[Input, Output]:

  def execute(
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]
