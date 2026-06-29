package domains.notification.objects

import io.circe.{Decoder, Encoder}


/** 通知业务类型枚举，目前用于区分博客回复通知。 */
enum NotificationKind:
  case BlogReply

/** 提供通知类型的线格式 codec。 */
object NotificationKind:
  given Encoder[NotificationKind] = Encoder.encodeString.contramap(encode)
  given Decoder[NotificationKind] = Decoder.decodeString.emap(parse)

  /** 解析通知类型线值，不支持的类型会解码失败。 */
  def parse(raw: String): Either[String, NotificationKind] =
    raw match
      case NotificationPayload.BlogReplyKind => Right(NotificationKind.BlogReply)
      case other => Left(s"Unsupported notification kind: $other")

  private def encode(kind: NotificationKind): String =
    kind match
      case NotificationKind.BlogReply => NotificationPayload.BlogReplyKind
