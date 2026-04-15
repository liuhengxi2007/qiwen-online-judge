package domains.usergroup.model

import domains.auth.model.Username

import java.time.Instant

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
