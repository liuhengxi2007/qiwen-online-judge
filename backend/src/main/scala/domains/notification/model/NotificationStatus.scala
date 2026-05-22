package domains.notification.model



enum NotificationStatus:
  case Unread
  case Read

object NotificationStatus:
  def parse(raw: String): Either[String, NotificationStatus] =
    raw match
      case "unread" => Right(NotificationStatus.Unread)
      case "read" => Right(NotificationStatus.Read)
      case other => Left(s"Unsupported notification status: $other")
