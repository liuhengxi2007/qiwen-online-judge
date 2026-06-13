package domains.auth.utils

import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import domains.auth.objects.SessionToken
import domains.auth.table.session.SessionTable
import domains.user.objects.Username
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Connection
import java.security.SecureRandom
import java.util.Base64

/** 会话存储上下文，携带数据库会话表、缓存和令牌生成/续期配置。 */
final case class SessionStoreContext(
  databaseSession: DatabaseSession,
  sessionConfig: SessionConfig,
  sessionCache: SessionCacheContext,
  random: SecureRandom
)

/** 会话存储函数集合，协调数据库会话表、缓存和令牌生成/续期。 */
object SessionStore:

  private val logger = Slf4jLogger.getLogger[IO]
  private val tokenLengthBytes = 32

  /** 创建 SessionStore 上下文；不会立即访问数据库。 */
  def create(databaseSession: DatabaseSession, sessionCache: SessionCacheContext): IO[SessionStoreContext] =
    IO.pure(SessionStoreContext(databaseSession, SessionConfig.default, sessionCache, SecureRandom()))

  /** 为用户创建新会话，写入数据库并尽力写入缓存，返回新令牌。 */
  def createSession(context: SessionStoreContext, username: Username): IO[SessionToken] =
    for
      token <- nextToken(context)
      now <- IO.realTimeInstant
      expiresAt = now.plus(context.sessionConfig.ttl)
      _ <- context.databaseSession.withTransactionConnection(connection =>
        insertSession(context, connection, token, username, expiresAt)
      )
      _ <- cachePut(context, token, CachedSession(username, expiresAt))
    yield token

  /** 在调用方事务连接中创建新会话，适合登录/注册等复合事务。 */
  def createSessionInConnection(context: SessionStoreContext, connection: Connection, username: Username): IO[SessionToken] =
    createSessionInConnection(context, connection, username, None)

  /** 根据令牌查找用户名；缓存用于快速解码，数据库仍作为会话有效性的最终来源。 */
  def lookupUsername(context: SessionStoreContext, token: SessionToken): IO[Option[Username]] =
    for
      now <- IO.realTimeInstant
      cached <- cacheGet(context, token)
      result <- cached match
        case Some(session) if session.expiresAt.isAfter(now) && !shouldRenew(context, session.expiresAt, now) =>
          loadAndRefreshCache(context, token, now)
        case Some(session) if session.expiresAt.isAfter(now) =>
          loadAndRefreshCache(context, token, now)
        case Some(_) =>
          cacheDelete(context, token) *> loadAndRefreshCache(context, token, now)
        case None =>
          loadAndRefreshCache(context, token, now)
    yield result

  /** 删除单个会话，同时清理数据库记录和缓存。 */
  def deleteSession(context: SessionStoreContext, token: SessionToken): IO[Unit] =
    context.databaseSession.withTransactionConnection(connection =>
      SessionTable.deleteByToken(connection, token)
    ) *> cacheDelete(context, token)

  /** 删除用户所有会话并逐个清理缓存，常用于密码变更后的强制下线。 */
  def deleteSessionsForUsername(context: SessionStoreContext, username: Username): IO[Unit] =
    context.databaseSession.withTransactionConnection(connection =>
      SessionTable.findTokensByUsername(connection, username).flatTap(_ => SessionTable.deleteByUsername(connection, username))
    ).flatMap(tokens => tokens.foldLeft(IO.unit)((effect, token) => effect *> cacheDelete(context, token)))

  private def nextToken(context: SessionStoreContext): IO[SessionToken] =
    IO.delay {
      val tokenBytes = new Array[Byte](tokenLengthBytes)
      context.random.nextBytes(tokenBytes)
      SessionToken(Base64.getUrlEncoder.withoutPadding().encodeToString(tokenBytes))
    }

  private def createSessionInConnection(
    context: SessionStoreContext,
    connection: Connection,
    username: Username,
    existingToken: Option[SessionToken]
  ): IO[SessionToken] =
    for
      token <- existingToken match
        case Some(token) => IO.pure(token)
        case None => nextToken(context)
      now <- IO.realTimeInstant
      expiresAt = now.plus(context.sessionConfig.ttl)
      _ <- insertSession(context, connection, token, username, expiresAt)
    yield token

  private def insertSession(
    context: SessionStoreContext,
    connection: Connection,
    token: SessionToken,
    username: Username,
    expiresAt: java.time.Instant
  ): IO[Unit] =
    SessionTable.deleteExpired(connection) *> SessionTable.insert(connection, token, username, expiresAt)

  private def loadAndMaybeRenewFromDatabase(
    context: SessionStoreContext,
    token: SessionToken,
    now: java.time.Instant
  ): IO[Option[SessionTable.ActiveSession]] =
    context.databaseSession.withTransactionConnection { connection =>
      SessionTable.findActiveByToken(connection, token, now).flatMap {
        case None =>
          IO.pure(None)
        case Some(activeSession) if !shouldRenew(context, activeSession.expiresAt, now) =>
          IO.pure(Some(activeSession))
        case Some(activeSession) =>
          val nextExpiresAt = now.plus(context.sessionConfig.ttl)
          SessionTable
            .renewSession(connection, token, now, nextExpiresAt)
            .map {
              case true => Some(activeSession.copy(expiresAt = nextExpiresAt))
              case false => None
            }
      }
    }

  private def loadAndRefreshCache(context: SessionStoreContext, token: SessionToken, now: java.time.Instant): IO[Option[Username]] =
    loadAndMaybeRenewFromDatabase(context, token, now).flatTap {
      case Some(activeSession) => cachePut(context, token, CachedSession(activeSession.username, activeSession.expiresAt))
      case None => cacheDelete(context, token)
    }.map(_.map(_.username))

  private def shouldRenew(context: SessionStoreContext, expiresAt: java.time.Instant, now: java.time.Instant): Boolean =
    !expiresAt.isAfter(now.plus(context.sessionConfig.renewalThreshold))

  private def cacheGet(context: SessionStoreContext, token: SessionToken): IO[Option[CachedSession]] =
    /** 注意：缓存读取失败按 miss 处理，避免 Redis 故障阻断登录态解析。 */
    SessionCache.get(context.sessionCache, token).handleErrorWith(_ => IO.pure(None))

  private def cachePut(context: SessionStoreContext, token: SessionToken, session: CachedSession): IO[Unit] =
    /** 注意：缓存写入是性能优化，失败不影响数据库会话已创建或续期的业务事实。 */
    SessionCache.put(context.sessionCache, token, session).handleErrorWith(_ => IO.unit)

  private def cacheDelete(context: SessionStoreContext, token: SessionToken): IO[Unit] =
    SessionCache.delete(context.sessionCache, token).handleErrorWith(error => logger.warn(error)("Failed to delete session cache entry.") *> IO.unit)
