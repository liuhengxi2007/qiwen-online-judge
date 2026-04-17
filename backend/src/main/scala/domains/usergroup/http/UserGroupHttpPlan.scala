package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser

import java.sql.Connection

trait UserGroupHttpPlan[Input, Output]:

  def name: String

trait PlainUserGroupHttpPlan[Input, Output] extends UserGroupHttpPlan[Input, Output]:

  def execute(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    input: Input
  ): IO[Output]

trait TransactionUserGroupHttpPlan[Input, Output] extends UserGroupHttpPlan[Input, Output]:

  def execute(
    connection: Connection,
    actor: AuthUser,
    input: Input
  ): IO[Output]
