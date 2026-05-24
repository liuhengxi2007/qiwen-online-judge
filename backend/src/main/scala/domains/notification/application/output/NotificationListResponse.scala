package domains.notification.application.output


final case class NotificationListResponse(
  notifications: List[NotificationSummary],
  unreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)
