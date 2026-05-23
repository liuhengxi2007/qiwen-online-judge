package domains.auth.application



import cats.effect.IO
import domains.auth.table.auth_user.AuthUserTable
import domains.user.model.Username

import java.sql.Connection

object AuthCommands:

  def validateUsername(username: Username): Either[String, Username] =
    UsernameRules.validate(username) match
      case Some(message) => Left(message)
      case None => Right(Username.canonical(username.value))

  def accessPolicyUserExists(connection: Connection, username: Username): IO[Boolean] =
    AuthUserTable.findByUsername(connection, username).map(_.nonEmpty)

  def resolveUserGroupMemberTarget(connection: Connection, username: Username): IO[Option[Username]] =
    AuthUserTable.findByUsername(connection, username).map(_.map(_.username))

  def usernameConflictsWithUser(connection: Connection, rawValue: String): IO[Boolean] =
    AuthUserTable.findByUsername(connection, Username.canonical(rawValue)).map(_.nonEmpty)

  def hashPassword(password: domains.auth.model.PlaintextPassword): IO[domains.auth.model.PasswordHash] =
    PasswordHasher.hashPassword(password)

  def verifyPassword(
    password: domains.auth.model.PlaintextPassword,
    passwordHash: domains.auth.model.PasswordHash
  ): IO[Boolean] =
    PasswordHasher.verifyPassword(password, passwordHash)
