package domains.auth.utils



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import domains.auth.table.session.SessionTable

import java.sql.Connection
import java.security.SecureRandom
import java.util.Base64

final class SessionStore private (
  databaseSession: DatabaseSession,
  sessionConfig: SessionConfig,
  sessionCache: SessionCache,
  random: SecureRandom
):

  private val tokenLengthBytes = 32

  def createSession(username: Username): IO[SessionToken] =
    for
      token <- nextToken
      now <- IO.realTimeInstant
      expiresAt = now.plus(sessionConfig.ttl)
      _ <- databaseSession.withTransactionConnection(connection =>
        insertSession(connection, token, username, expiresAt)
      )
      _ <- cachePut(token, CachedSession(username, expiresAt))
    yield token

  def createSessionInConnection(connection: Connection, username: Username): IO[SessionToken] =
    createSessionInConnection(connection, username, None)

  def lookupUsername(token: SessionToken): IO[Option[Username]] =
    for
      now <- IO.realTimeInstant
      cached <- cacheGet(token)
      result <- cached match
        case Some(session) if session.expiresAt.isAfter(now) && !shouldRenew(session.expiresAt, now) =>
          IO.pure(Some(session.username))
        case Some(session) if session.expiresAt.isAfter(now) =>
          loadAndMaybeRenewFromDatabase(token, now).flatTap {
            case Some(activeSession) => cachePut(token, CachedSession(activeSession.username, activeSession.expiresAt))
            case None => cacheDelete(token)
          }.map(_.map(_.username))
        case Some(_) =>
          cacheDelete(token) *> loadAndMaybeRenewFromDatabase(token, now).flatTap {
            case Some(activeSession) => cachePut(token, CachedSession(activeSession.username, activeSession.expiresAt))
            case None => IO.unit
          }.map(_.map(_.username))
        case None =>
          loadAndMaybeRenewFromDatabase(token, now).flatTap {
            case Some(activeSession) => cachePut(token, CachedSession(activeSession.username, activeSession.expiresAt))
            case None => IO.unit
          }.map(_.map(_.username))
    yield result

  def deleteSession(token: SessionToken): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByToken(connection, token)
    ) *> cacheDelete(token)

  def deleteSessionsForUsername(username: Username): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.findTokensByUsername(connection, username).flatTap(_ => SessionTable.deleteByUsername(connection, username))
    ).flatMap(tokens => tokens.foldLeft(IO.unit)((effect, token) => effect *> cacheDelete(token)))

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
      now <- IO.realTimeInstant
      expiresAt = now.plus(sessionConfig.ttl)
      _ <- insertSession(connection, token, username, expiresAt)
    yield token

  private def insertSession(
    connection: Connection,
    token: SessionToken,
    username: Username,
    expiresAt: java.time.Instant
  ): IO[Unit] =
    SessionTable.deleteExpired(connection) *> SessionTable.insert(connection, token, username, expiresAt)

  private def loadAndMaybeRenewFromDatabase(
    token: SessionToken,
    now: java.time.Instant
  ): IO[Option[SessionTable.ActiveSession]] =
    databaseSession.withTransactionConnection { connection =>
      SessionTable.findActiveByToken(connection, token, now).flatMap {
        case None =>
          IO.pure(None)
        case Some(activeSession) if !shouldRenew(activeSession.expiresAt, now) =>
          IO.pure(Some(activeSession))
        case Some(activeSession) =>
          val nextExpiresAt = now.plus(sessionConfig.ttl)
          SessionTable
            .renewSession(connection, token, now, nextExpiresAt)
            .map {
              case true => Some(activeSession.copy(expiresAt = nextExpiresAt))
              case false => None
            }
      }
    }

  private def shouldRenew(expiresAt: java.time.Instant, now: java.time.Instant): Boolean =
    !expiresAt.isAfter(now.plus(sessionConfig.renewalThreshold))

  private def cacheGet(token: SessionToken): IO[Option[CachedSession]] =
    sessionCache.get(token).handleErrorWith(_ => IO.pure(None))

  private def cachePut(token: SessionToken, session: CachedSession): IO[Unit] =
    sessionCache.put(token, session).handleErrorWith(_ => IO.unit)

  private def cacheDelete(token: SessionToken): IO[Unit] =
    sessionCache.delete(token).handleErrorWith(_ => IO.unit)

object SessionStore:

  def create(databaseSession: DatabaseSession, sessionCache: SessionCache): IO[SessionStore] =
    IO.pure(new SessionStore(databaseSession, SessionConfig.default, sessionCache, SecureRandom()))
