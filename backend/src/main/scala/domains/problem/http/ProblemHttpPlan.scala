package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import org.http4s.Response

trait ProblemHttpPlan[Input]:

  def name: String

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Response[IO]]
