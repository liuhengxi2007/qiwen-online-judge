package domains.notification.api

/** 通知 SSE 事件集合，当前仅表示通知状态发生变化。 */
enum NotificationStreamEvent:
  case NotificationsChanged
