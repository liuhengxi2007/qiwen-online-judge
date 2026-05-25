package domains.usergroup.model.request

import domains.usergroup.model.*

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)
