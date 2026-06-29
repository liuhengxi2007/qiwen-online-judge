package domains.usergroup.objects



import domains.user.objects.Username

import java.time.Instant

/** 用户组完整内部模型，包含基础资料、所有者和成员列表。 */
final case class UserGroup(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  members: List[UserGroupMember],
  createdAt: Instant,
  updatedAt: Instant
)
