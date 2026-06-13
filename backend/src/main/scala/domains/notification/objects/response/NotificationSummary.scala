package domains.notification.objects.response

import domains.notification.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 通知摘要响应，包含渲染 key、payload、跳转目标、读取状态和创建时间。 */
final case class NotificationSummary(
  id: NotificationId,
  kind: NotificationKind,
  actor: Option[UserIdentity],
  titleKey: String,
  bodyKey: String,
  payload: NotificationPayload,
  targetPath: String,
  targetAnchor: Option[String],
  isRead: Boolean,
  createdAt: Instant
)

/** 提供通知摘要 JSON codec，并显式处理 Instant 字符串格式。 */
object NotificationSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[NotificationSummary] = deriveEncoder[NotificationSummary]
  given Decoder[NotificationSummary] = deriveDecoder[NotificationSummary]
