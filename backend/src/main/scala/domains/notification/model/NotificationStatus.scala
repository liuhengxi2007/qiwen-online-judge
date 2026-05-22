package domains.notification.model



enum NotificationStatus:
  case Unread
  case Read

object NotificationStatus:
  def toDatabase(status: NotificationStatus): String =
    status match
      case NotificationStatus.Unread => "unread"
      case NotificationStatus.Read => "read"

  def parse(raw: String): Either[String, NotificationStatus] =
    raw match
      case "unread" => Right(NotificationStatus.Unread)
      case "read" => Right(NotificationStatus.Read)
      case other => Left(s"Unsupported notification status: $other")
