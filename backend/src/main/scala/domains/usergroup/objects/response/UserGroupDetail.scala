package domains.usergroup.objects.response

import domains.usergroup.objects.*

import domains.user.objects.Username

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
