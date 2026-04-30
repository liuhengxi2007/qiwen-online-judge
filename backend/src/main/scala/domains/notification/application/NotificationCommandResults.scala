package domains.notification.application

object NotificationCommandResults:

  enum MarkNotificationReadResult:
    case NotFound
    case Marked

  enum MarkAllNotificationsReadResult:
    case Marked
