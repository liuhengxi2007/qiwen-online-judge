package domains.notification.utils



sealed trait NotificationStreamEvent

object NotificationStreamEvent:
  case object NotificationsChanged extends NotificationStreamEvent
