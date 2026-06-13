package domains.notification.utils



/** 通知 SSE 内部事件基类，当前仅表示通知状态发生变化。 */
sealed trait NotificationStreamEvent

/** 通知 SSE 事件集合。 */
object NotificationStreamEvent:
  case object NotificationsChanged extends NotificationStreamEvent
