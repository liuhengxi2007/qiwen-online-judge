package domains.usergroup.model.request

import domains.usergroup.model.*

final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)
