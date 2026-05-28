package domains.notification.objects

import io.circe.{Decoder, Encoder}


enum NotificationKind:
  case BlogReply

object NotificationKind:
  given Encoder[NotificationKind] = Encoder.encodeString.contramap(encode)
  given Decoder[NotificationKind] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, NotificationKind] =
    raw match
      case NotificationPayload.BlogReplyKind => Right(NotificationKind.BlogReply)
      case other => Left(s"Unsupported notification kind: $other")

  private def encode(kind: NotificationKind): String =
    kind match
      case NotificationKind.BlogReply => NotificationPayload.BlogReplyKind
