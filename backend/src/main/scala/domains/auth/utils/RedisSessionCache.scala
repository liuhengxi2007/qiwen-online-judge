package domains.auth.utils



import cats.effect.{IO, Resource}
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import io.circe.Json
import io.circe.parser.parse
import redis.clients.jedis.JedisPooled

import java.time.Instant

/** Redis 会话缓存实现，缓存 token 到用户名和过期时间的映射。 */
final class RedisSessionCache private (client: JedisPooled) extends SessionCache:
  /** 从 Redis 读取并解码会话，缓存内容非法时会删除该 key。 */
  override def get(token: SessionToken): IO[Option[CachedSession]] =
    IO.blocking(Option(client.get(key(token)))).flatMap {
      case None => IO.pure(None)
      case Some(raw) =>
        decode(raw) match
          case Some(session) => IO.pure(Some(session))
          case None => delete(token).as(None)
    }

  /** 以会话剩余时间作为 Redis TTL 写入缓存。 */
  override def put(token: SessionToken, session: CachedSession): IO[Unit] =
    IO.realTimeInstant.flatMap { now =>
      val ttlSeconds = math.max(1L, java.time.Duration.between(now, session.expiresAt).getSeconds)
      IO.blocking {
        client.setex(key(token), ttlSeconds, encode(session))
        ()
      }
    }

  /** 删除 Redis 中的会话缓存 key，删除不存在的 key 也成功。 */
  override def delete(token: SessionToken): IO[Unit] =
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
      yield CachedSession(
        // FIXME-CN: Redis 缓存用户名使用 canonical 而不是 parse，篡改或历史脏数据可绕过用户名格式约束。
        username = Username.canonical(username),
        expiresAt = Instant.ofEpochMilli(expiresAtEpochMilli)
      )
    }

/** Redis 会话缓存资源构造器，负责 Jedis 客户端生命周期。 */
object RedisSessionCache:
  /** 根据配置创建 Redis-backed SessionCache 资源，释放时关闭客户端。 */
  def resource(config: SessionCacheConfig): Resource[IO, SessionCache] =
    Resource
      .make(IO.blocking(new JedisPooled(config.redisUrl)))(client =>
        IO.blocking(client.close()).handleErrorWith(_ => IO.unit)
      )
      .map(client => new RedisSessionCache(client))
