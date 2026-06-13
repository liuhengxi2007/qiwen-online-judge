package domains.notification.objects.internal

/** 通知读取状态枚举，与 notifications.status 数据库列对应。 */
enum NotificationStatus:
  case Unread
  case Read

/** 提供通知读取状态数据库线值解析。 */
object NotificationStatus:
  /** 解析 unread/read 状态，不支持的状态视为数据异常。 */
  def parse(raw: String): Either[String, NotificationStatus] =
    raw match
      case "unread" => Right(NotificationStatus.Unread)
      case "read" => Right(NotificationStatus.Read)
      case other => Left(s"Unsupported notification status: $other")
