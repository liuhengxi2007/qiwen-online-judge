package domains.auth.application



import java.time.Duration

final case class SessionConfig(
  ttl: Duration
):
  val renewalThreshold: Duration = ttl.dividedBy(2)

object SessionConfig:

  val default: SessionConfig =
    SessionConfig(
      ttl = Duration.ofDays(sys.env.get("AUTH_SESSION_TTL_DAYS").flatMap(_.toLongOption).getOrElse(3L))
    )
