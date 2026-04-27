package domains.auth.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{SessionToken, Username}
import domains.auth.table.SessionTable

import java.sql.Connection
import java.security.SecureRandom
import java.util.Base64

final class SessionStore private (
  databaseSession: DatabaseSession,
  sessionConfig: SessionConfig,
  random: SecureRandom
):

  private val tokenLengthBytes = 32

  def createSession(username: Username): IO[SessionToken] =
    for
      token <- nextToken
      _ <- databaseSession.withTransactionConnection(connection =>
        createSessionInConnection(connection, username, Some(token)).void
      )
    yield token

  def createSessionInConnection(connection: Connection, username: Username): IO[SessionToken] =
    createSessionInConnection(connection, username, None)

  def lookupUsername(token: SessionToken): IO[Option[Username]] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteExpired(connection) *>
        SessionTable.touchAndFindUsernameByToken(connection, token, sessionConfig.activeExtensionThreshold)
    )

  def deleteSession(token: SessionToken): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByToken(connection, token)
    )

  def deleteSessionsForUsername(username: Username): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByUsername(connection, username)
    )

  private def nextToken: IO[SessionToken] =
    IO.delay {
      val tokenBytes = new Array[Byte](tokenLengthBytes)
      random.nextBytes(tokenBytes)
      SessionToken(Base64.getUrlEncoder.withoutPadding().encodeToString(tokenBytes))
    }

  private def createSessionInConnection(
    connection: Connection,
    username: Username,
    existingToken: Option[SessionToken]
  ): IO[SessionToken] =
    for
      token <- existingToken match
        case Some(token) => IO.pure(token)
        case None => nextToken
      expiresAt = java.time.Instant.now().plus(sessionConfig.ttl)
      _ <- insertSession(connection, token, username, expiresAt)
    yield token

  private def insertSession(
    connection: Connection,
    token: SessionToken,
    username: Username,
    expiresAt: java.time.Instant
  ): IO[Unit] =
    SessionTable.deleteExpired(connection) *> SessionTable.insert(connection, token, username, expiresAt)

object SessionStore:

  def create(databaseSession: DatabaseSession): IO[SessionStore] =
    IO.pure(new SessionStore(databaseSession, SessionConfig.default, SecureRandom()))
