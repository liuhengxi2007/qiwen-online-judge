package domains.auth.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.{AuthUser, SiteManagerUser}

import java.sql.Connection

final case class AuthHttpContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)

trait AuthHttpPlan[Input, Output]:

  def name: String

trait PublicPlainAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    input: Input
  ): IO[Output]

trait PublicTransactionAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    connection: Connection,
    input: Input
  ): IO[Output]

trait AuthenticatedPlainAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait AuthenticatedTransactionAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait SiteManagerPlainAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    actor: SiteManagerUser,
    input: Input
  ): IO[Output]

trait SiteManagerTransactionAuthHttpPlan[Input, Output] extends AuthHttpPlan[Input, Output]:

  def execute(
    context: AuthHttpContext,
    connection: Connection,
    actor: SiteManagerUser,
    input: Input
  ): IO[Output]
