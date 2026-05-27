package domains.notification.objects



enum NotificationKind:
  case BlogReply

object NotificationKind:
  def parse(raw: String): Either[String, NotificationKind] =
    raw match
      case NotificationPayload.BlogReplyKind => Right(NotificationKind.BlogReply)
      case other => Left(s"Unsupported notification kind: $other")
