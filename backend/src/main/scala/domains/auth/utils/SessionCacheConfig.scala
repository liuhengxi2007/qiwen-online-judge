package domains.auth.utils



/** 会话缓存配置，目前仅包含 Redis 连接 URL。 */
final case class SessionCacheConfig(
  redisUrl: String
)

/** 从环境变量加载可选会话缓存配置。 */
object SessionCacheConfig:
  /** 环境中存在非空 AUTH_SESSION_CACHE_REDIS_URL 时启用 Redis 缓存。 */
  def fromEnvironment(env: scala.collection.Map[String, String]): Option[SessionCacheConfig] =
    env
      .get("AUTH_SESSION_CACHE_REDIS_URL")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(SessionCacheConfig(_))
