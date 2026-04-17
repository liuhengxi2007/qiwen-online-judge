package domains.shared.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser

import java.sql.Connection

trait AuthenticatedHttpPlan[Input, Output]:

  def name: String

trait PlainAuthenticatedHttpPlan[Input, Output] extends AuthenticatedHttpPlan[Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait TransactionAuthenticatedHttpPlan[Input, Output] extends AuthenticatedHttpPlan[Input, Output]:

  def execute(
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]

object AuthenticatedHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class Plain[Input, Output](
      plan: PlainAuthenticatedHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class WithTransaction[Input, Output](
      plan: TransactionAuthenticatedHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
