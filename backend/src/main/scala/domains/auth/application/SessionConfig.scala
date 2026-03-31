package domains.auth.application

import java.time.Duration

final case class SessionConfig(
  ttl: Duration,
  activeExtensionThreshold: Duration
)

object SessionConfig:

  val default: SessionConfig =
    SessionConfig(
      ttl = Duration.ofDays(sys.env.get("AUTH_SESSION_TTL_DAYS").flatMap(_.toLongOption).getOrElse(3L)),
      activeExtensionThreshold =
        Duration.ofHours(
          sys.env.get("AUTH_SESSION_ACTIVE_EXTENSION_HOURS").flatMap(_.toLongOption).getOrElse(3L)
        )
    )
