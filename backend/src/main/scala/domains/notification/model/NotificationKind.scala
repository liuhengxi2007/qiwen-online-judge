package domains.notification.model



import io.circe.{Decoder, Encoder}

enum NotificationKind:
  case BlogReply

object NotificationKind:
  def toDatabase(kind: NotificationKind): String =
    kind match
      case NotificationKind.BlogReply => "blog_reply"

  def parse(raw: String): Either[String, NotificationKind] =
    raw match
      case "blog_reply" => Right(NotificationKind.BlogReply)
      case other => Left(s"Unsupported notification kind: $other")

  given Encoder[NotificationKind] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[NotificationKind] = Decoder.decodeString.emap(parse)
