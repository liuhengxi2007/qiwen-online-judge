package domains.notification.model.response

import domains.notification.model.*

import domains.user.model.UserIdentity

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
