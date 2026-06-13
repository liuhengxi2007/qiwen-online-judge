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
    SessionConfig(
      // FIXME-CN: AUTH_SESSION_TTL_DAYS 只做 Long 解析，0 或负数会生成立即过期或异常的会话生命周期。
      ttl = Duration.ofDays(sys.env.get("AUTH_SESSION_TTL_DAYS").flatMap(_.toLongOption).getOrElse(3L))
    )
