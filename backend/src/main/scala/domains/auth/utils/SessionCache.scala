package domains.auth.utils

import cats.effect.{IO, Resource}
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import io.circe.Json
import io.circe.parser.parse
import redis.clients.jedis.JedisPooled

import java.time.Instant

/** 会话缓存值，保存用户名和服务端会话过期时间。 */
final case class CachedSession(
  username: Username,
  expiresAt: Instant
)

/** 会话缓存上下文；None 表示禁用 Redis 并按缓存 miss 处理。 */
final case class SessionCacheContext(redisClient: Option[JedisPooled])

/** 会话缓存函数集合，允许用 Redis 或空上下文加速令牌查找。 */
object SessionCache:

  val noop: SessionCacheContext = SessionCacheContext(None)

  /** 根据配置创建 Redis-backed SessionCache 资源，释放时关闭客户端。 */
  def resource(config: SessionCacheConfig): Resource[IO, SessionCacheContext] =
    Resource
      .make(IO.blocking(new JedisPooled(config.redisUrl)))(client =>
        IO.blocking(client.close()).handleErrorWith(_ => IO.unit)
      )
      .map(client => SessionCacheContext(Some(client)))

  /** 根据令牌读取缓存会话，缓存缺失返回 None；缓存内容非法时会删除该 key。 */
  def get(context: SessionCacheContext, token: SessionToken): IO[Option[CachedSession]] =
    context.redisClient match
      case None => IO.pure(None)
      case Some(client) =>
        IO.blocking(Option(client.get(key(token)))).flatMap {
          case None => IO.pure(None)
          case Some(raw) =>
            decode(raw) match
              case Some(session) => IO.pure(Some(session))
              case None => delete(context, token).as(None)
        }

  /** 以会话剩余时间作为 Redis TTL 写入缓存；空上下文无副作用。 */
  def put(context: SessionCacheContext, token: SessionToken, session: CachedSession): IO[Unit] =
    context.redisClient match
      case None => IO.unit
      case Some(client) =>
        IO.realTimeInstant.flatMap { now =>
          val ttlSeconds = math.max(1L, java.time.Duration.between(now, session.expiresAt).getSeconds)
          IO.blocking {
            client.setex(key(token), ttlSeconds, encode(session))
            ()
          }
        }

  /** 删除 Redis 中的会话缓存 key，删除不存在的 key 也成功；空上下文无副作用。 */
  def delete(context: SessionCacheContext, token: SessionToken): IO[Unit] =
    context.redisClient match
      case None => IO.unit
      case Some(client) =>
        IO.blocking {
          client.del(key(token))
          ()
        }

  private def key(token: SessionToken): String =
    s"session:${token.value}"

  private def encode(session: CachedSession): String =
    Json
      .obj(
        "username" -> Json.fromString(session.username.value),
        "expiresAtEpochMilli" -> Json.fromLong(session.expiresAt.toEpochMilli)
      )
      .noSpaces

  private def decode(raw: String): Option[CachedSession] =
    parse(raw).toOption.flatMap { json =>
      for
        username <- json.hcursor.get[String]("username").toOption
        expiresAtEpochMilli <- json.hcursor.get[Long]("expiresAtEpochMilli").toOption
        parsedUsername <- Username.parse(username).toOption
      yield CachedSession(
        username = parsedUsername,
        expiresAt = Instant.ofEpochMilli(expiresAtEpochMilli)
      )
    }
