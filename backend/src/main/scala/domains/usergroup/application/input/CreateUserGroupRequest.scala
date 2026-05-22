package domains.usergroup.application.input

import domains.usergroup.model.*

final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)
