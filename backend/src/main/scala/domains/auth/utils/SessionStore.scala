package domains.auth.utils



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import domains.auth.table.session.SessionTable
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Connection
import java.security.SecureRandom
import java.util.Base64

/** 会话存储服务，协调数据库会话表、缓存和令牌生成/续期。 */
final class SessionStore private (
  databaseSession: DatabaseSession,
  sessionConfig: SessionConfig,
  sessionCache: SessionCache,
  random: SecureRandom
):

  private val tokenLengthBytes = 32

  /** 为用户创建新会话，写入数据库并尽力写入缓存，返回新令牌。 */
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

  /** 在调用方事务连接中创建新会话，适合登录/注册等复合事务。 */
  def createSessionInConnection(connection: Connection, username: Username): IO[SessionToken] =
    createSessionInConnection(connection, username, None)

  /** 根据令牌查找用户名；缓存用于快速解码，数据库仍作为会话有效性的最终来源。 */
  def lookupUsername(token: SessionToken): IO[Option[Username]] =
    for
      now <- IO.realTimeInstant
      cached <- cacheGet(token)
      result <- cached match
        case Some(session) if session.expiresAt.isAfter(now) && !shouldRenew(session.expiresAt, now) =>
          loadAndRefreshCache(token, now)
        case Some(session) if session.expiresAt.isAfter(now) =>
          loadAndRefreshCache(token, now)
        case Some(_) =>
          cacheDelete(token) *> loadAndRefreshCache(token, now)
        case None =>
          loadAndRefreshCache(token, now)
    yield result

  /** 删除单个会话，同时清理数据库记录和缓存。 */
  def deleteSession(token: SessionToken): IO[Unit] =
    databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByToken(connection, token)
    ) *> cacheDelete(token)

  /** 删除用户所有会话并逐个清理缓存，常用于密码变更后的强制下线。 */
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

  private def loadAndRefreshCache(token: SessionToken, now: java.time.Instant): IO[Option[Username]] =
    loadAndMaybeRenewFromDatabase(token, now).flatTap {
      case Some(activeSession) => cachePut(token, CachedSession(activeSession.username, activeSession.expiresAt))
      case None => cacheDelete(token)
    }.map(_.map(_.username))

  private def shouldRenew(expiresAt: java.time.Instant, now: java.time.Instant): Boolean =
    !expiresAt.isAfter(now.plus(sessionConfig.renewalThreshold))

  private def cacheGet(token: SessionToken): IO[Option[CachedSession]] =
    /** 注意：缓存读取失败按 miss 处理，避免 Redis 故障阻断登录态解析。 */
    sessionCache.get(token).handleErrorWith(_ => IO.pure(None))

  private def cachePut(token: SessionToken, session: CachedSession): IO[Unit] =
    /** 注意：缓存写入是性能优化，失败不影响数据库会话已创建或续期的业务事实。 */
    sessionCache.put(token, session).handleErrorWith(_ => IO.unit)

  private def cacheDelete(token: SessionToken): IO[Unit] =
    sessionCache.delete(token).handleErrorWith(error => SessionStore.logger.warn(error)("Failed to delete session cache entry.") *> IO.unit)

/** 会话存储服务构造器，装配默认会话配置和安全随机数源。 */
object SessionStore:

  private val logger = Slf4jLogger.getLogger[IO]

  /** 创建 SessionStore 实例；不会立即访问数据库。 */
  def create(databaseSession: DatabaseSession, sessionCache: SessionCache): IO[SessionStore] =
    IO.pure(new SessionStore(databaseSession, SessionConfig.default, sessionCache, SecureRandom()))
