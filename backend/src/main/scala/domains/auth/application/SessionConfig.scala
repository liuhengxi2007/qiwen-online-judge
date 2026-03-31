package domains.auth.application

import java.time.Duration

final case class SessionConfig(
  ttl: Duration,
  activeExtensionThreshold: Duration
)

object SessionConfig:

  val default: SessionConfig =
    SessionConfig(
      ttl = Duration.ofMinutes(sys.env.get("AUTH_SESSION_TTL_MINUTES").flatMap(_.toLongOption).getOrElse(1L)),
      activeExtensionThreshold =
        Duration.ofMinutes(
          sys.env.get("AUTH_SESSION_ACTIVE_EXTENSION_MINUTES").flatMap(_.toLongOption).getOrElse(1L)
        )
    )
