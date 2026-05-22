package domains.usergroup.application.output

import domains.usergroup.model.*

import domains.user.model.Username

import java.time.Instant

final case class UserGroupDetail(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  members: List[UserGroupMember],
  createdAt: Instant,
  updatedAt: Instant
)
