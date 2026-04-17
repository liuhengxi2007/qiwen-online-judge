package domains.problemset.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser

import java.sql.Connection

trait ProblemSetHttpPlan[Input, Output]:

  def name: String

trait PlainProblemSetHttpPlan[Input, Output] extends ProblemSetHttpPlan[Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait TransactionProblemSetHttpPlan[Input, Output] extends ProblemSetHttpPlan[Input, Output]:

  def execute(
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]
