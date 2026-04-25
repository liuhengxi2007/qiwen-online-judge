package domains.user.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.{AuthUser, SiteManagerUser}

import java.sql.Connection

final case class UserHttpContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)

trait UserHttpPlan[Input, Output]:
  def name: String

trait AuthenticatedPlainUserHttpPlan[Input, Output] extends UserHttpPlan[Input, Output]:
  def execute(context: UserHttpContext, actor: AuthUser, input: Input): IO[Output]

trait AuthenticatedTransactionUserHttpPlan[Input, Output] extends UserHttpPlan[Input, Output]:
  def execute(context: UserHttpContext, connection: Connection, actor: AuthUser, input: Input): IO[Output]

trait SiteManagerPlainUserHttpPlan[Input, Output] extends UserHttpPlan[Input, Output]:
  def execute(context: UserHttpContext, actor: SiteManagerUser, input: Input): IO[Output]

trait SiteManagerTransactionUserHttpPlan[Input, Output] extends UserHttpPlan[Input, Output]:
  def execute(context: UserHttpContext, connection: Connection, actor: SiteManagerUser, input: Input): IO[Output]

object UserHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class AuthenticatedPlain[Input, Output](
      plan: AuthenticatedPlainUserHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class AuthenticatedWithTransaction[Input, Output](
      plan: AuthenticatedTransactionUserHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class SiteManagerPlain[Input, Output](
      plan: SiteManagerPlainUserHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class SiteManagerWithTransaction[Input, Output](
      plan: SiteManagerTransactionUserHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
