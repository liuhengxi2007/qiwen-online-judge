package domains.notification.application.output

import domains.notification.model.*

final case class NotificationListResponse(
  notifications: List[NotificationSummary],
  unreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)
