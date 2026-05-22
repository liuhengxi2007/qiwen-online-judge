package domains.judger.application.output

import java.time.Instant

final case class RegisteredJudgerListItem(
  judgerId: String,
  requestedPrefix: String,
  host: String,
  processId: Option[String],
  supportedLanguages: List[String],
  registeredAt: Instant,
  lastHeartbeatAt: Instant
)
