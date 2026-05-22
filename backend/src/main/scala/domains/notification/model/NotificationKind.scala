package domains.notification.model



enum NotificationKind:
  case BlogReply

object NotificationKind:
  def toDatabase(kind: NotificationKind): String =
    kind match
      case NotificationKind.BlogReply => NotificationPayload.BlogReplyKind

  def parse(raw: String): Either[String, NotificationKind] =
    raw match
      case NotificationPayload.BlogReplyKind => Right(NotificationKind.BlogReply)
      case other => Left(s"Unsupported notification kind: $other")
