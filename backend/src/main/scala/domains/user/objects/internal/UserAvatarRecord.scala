package domains.user.objects.internal

import domains.user.objects.Username

import java.time.Instant

final case class UserAvatarRecord(
  username: Username,
  objectKey: String,
  contentType: String,
  updatedAt: Instant
)
