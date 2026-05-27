package domains.auth.application



import cats.effect.{IO, Resource}
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import io.circe.Json
import io.circe.parser.parse
import redis.clients.jedis.JedisPooled

import java.time.Instant

final class RedisSessionCache private (client: JedisPooled) extends SessionCache:
  override def get(token: SessionToken): IO[Option[CachedSession]] =
    IO.blocking(Option(client.get(key(token)))).flatMap {
      case None => IO.pure(None)
      case Some(raw) =>
        decode(raw) match
          case Some(session) => IO.pure(Some(session))
          case None => delete(token).as(None)
    }

  override def put(token: SessionToken, session: CachedSession): IO[Unit] =
    IO.realTimeInstant.flatMap { now =>
      val ttlSeconds = math.max(1L, java.time.Duration.between(now, session.expiresAt).getSeconds)
      IO.blocking {
        client.setex(key(token), ttlSeconds, encode(session))
        ()
      }
    }

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
        username = Username.canonical(username),
        expiresAt = Instant.ofEpochMilli(expiresAtEpochMilli)
      )
    }

object RedisSessionCache:
  def resource(config: SessionCacheConfig): Resource[IO, SessionCache] =
    Resource
      .make(IO.blocking(new JedisPooled(config.redisUrl)))(client =>
        IO.blocking(client.close()).handleErrorWith(_ => IO.unit)
      )
      .map(client => new RedisSessionCache(client))
