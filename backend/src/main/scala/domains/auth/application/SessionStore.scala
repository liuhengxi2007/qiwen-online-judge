package domains.auth.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.Username
import domains.auth.table.SessionTable

import java.security.SecureRandom
import java.util.Base64

final class SessionStore private (
  databaseSession: DatabaseSession,
  sessionConfig: SessionConfig,
  random: SecureRandom
):

  private val tokenLengthBytes = 32

  def createSession(username: Username): IO[String] =
    for
      token <- nextToken
      expiresAt = java.time.Instant.now().plus(sessionConfig.ttl)
      _ <- databaseSession.withTransactionConnection(connection =>
        SessionTable.deleteExpired(connection) *> SessionTable.insert(connection, token, username, expiresAt)
      )
    yield token

  def lookupUsername(token: String): IO[Option[Username]] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteExpired(connection) *>
        SessionTable.touchAndFindUsernameByToken(connection, token, sessionConfig.activeExtensionThreshold)
    )

  def deleteSession(token: String): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByToken(connection, token)
    )

  def deleteSessionsForUsername(username: Username): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByUsername(connection, username)
    )

  private def nextToken: IO[String] =
    IO.delay {
      val tokenBytes = new Array[Byte](tokenLengthBytes)
      random.nextBytes(tokenBytes)
      Base64.getUrlEncoder.withoutPadding().encodeToString(tokenBytes)
    }

object SessionStore:

  def create(databaseSession: DatabaseSession): IO[SessionStore] =
    IO.pure(new SessionStore(databaseSession, SessionConfig.default, SecureRandom()))
