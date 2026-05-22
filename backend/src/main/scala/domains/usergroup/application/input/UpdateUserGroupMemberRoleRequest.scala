package domains.usergroup.application.input

import domains.usergroup.model.*

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)
