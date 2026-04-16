package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import org.http4s.Response

import java.sql.Connection

trait ProblemHttpPlan[Input, Output]:

  def name: String

trait PlainProblemHttpPlan[Input, Output] extends ProblemHttpPlan[Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait TransactionProblemHttpPlan[Input, Output] extends ProblemHttpPlan[Input, Output]:

  def execute(
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]
