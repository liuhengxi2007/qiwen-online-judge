package domains.usergroup.model.request

import domains.usergroup.model.*

final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)
