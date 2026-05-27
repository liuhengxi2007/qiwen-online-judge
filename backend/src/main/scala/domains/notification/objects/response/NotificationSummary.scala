package domains.notification.objects.response

import domains.notification.objects.*

import domains.user.objects.UserIdentity

import java.time.Instant

final case class NotificationSummary(
  id: NotificationId,
  kind: NotificationKind,
  actor: Option[UserIdentity],
  titleKey: String,
  bodyKey: String,
  payload: NotificationPayload,
  targetPath: String,
  targetAnchor: Option[String],
  isRead: Boolean,
  createdAt: Instant
)
