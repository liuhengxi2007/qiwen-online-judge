package domains.usergroup.application.input

import domains.usergroup.model.*

final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)
