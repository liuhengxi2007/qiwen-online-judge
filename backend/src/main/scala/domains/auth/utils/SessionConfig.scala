package domains.auth.utils



import java.time.Duration

/** 会话生命周期配置，ttl 控制服务端会话和 cookie 对应令牌的有效期。 */
final case class SessionConfig(
  ttl: Duration
):
  val renewalThreshold: Duration = ttl.dividedBy(2)

/** 会话配置加载器，默认从环境变量读取 TTL 天数。 */
object SessionConfig:

  val default: SessionConfig =
    val ttlDays = sys.env.get("AUTH_SESSION_TTL_DAYS").flatMap(_.toLongOption).getOrElse(3L)
    require(ttlDays > 0, "AUTH_SESSION_TTL_DAYS must be greater than 0.")
    SessionConfig(
      ttl = Duration.ofDays(ttlDays)
    )
