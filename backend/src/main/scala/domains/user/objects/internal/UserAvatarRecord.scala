package domains.user.objects.internal

import domains.user.objects.Username

import java.time.Instant

/** 用户头像元数据记录，描述对象存储 key、内容类型和更新时间。 */
final case class UserAvatarRecord(
  username: Username,
  objectKey: String,
  contentType: String,
  updatedAt: Instant
)
