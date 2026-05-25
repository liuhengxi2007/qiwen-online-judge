package shared.http



import cats.effect.IO
import database.DatabaseSession

import java.sql.Connection

trait AuthenticatedHttpPlan[Actor, Input, Output]

trait PlainAuthenticatedHttpPlan[Actor, Input, Output] extends AuthenticatedHttpPlan[Actor, Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: Actor,
    input: Input
  ): IO[Output]

trait TransactionAuthenticatedHttpPlan[Actor, Input, Output] extends AuthenticatedHttpPlan[Actor, Input, Output]:

  def execute(
    connection: Connection,
    actor: Actor,
    input: Input
  ): IO[Output]

object AuthenticatedHttpPlanRegistry:

  sealed trait RegisteredPlan

  object RegisteredPlan:

    final case class Plain[Actor, Input, Output](
      plan: PlainAuthenticatedHttpPlan[Actor, Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan

    final case class WithTransaction[Actor, Input, Output](
      plan: TransactionAuthenticatedHttpPlan[Actor, Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan
