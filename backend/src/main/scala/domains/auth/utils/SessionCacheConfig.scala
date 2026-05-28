package domains.auth.utils



final case class SessionCacheConfig(
  redisUrl: String
)

object SessionCacheConfig:
  def fromEnvironment(env: scala.collection.Map[String, String]): Option[SessionCacheConfig] =
    env
      .get("AUTH_SESSION_CACHE_REDIS_URL")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(SessionCacheConfig(_))
