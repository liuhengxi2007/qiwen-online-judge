package domains.notification.application

sealed trait NotificationStreamEvent

object NotificationStreamEvent:
  case object NotificationsChanged extends NotificationStreamEvent
