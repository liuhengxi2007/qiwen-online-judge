package domains.usergroup.application.output

import domains.usergroup.model.*

import domains.user.model.Username

import java.time.Instant

final case class UserGroupSummary(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  createdAt: Instant,
  updatedAt: Instant
)
