package domains.auth.application

import java.time.Duration

final case class SessionConfig(
  ttl: Duration
)

object SessionConfig:

  val default: SessionConfig =
    SessionConfig(
      ttl = Duration.ofMinutes(sys.env.get("AUTH_SESSION_TTL_MINUTES").flatMap(_.toLongOption).getOrElse(1L))
    )
