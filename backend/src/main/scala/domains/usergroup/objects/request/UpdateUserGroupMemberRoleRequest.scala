package domains.usergroup.objects.request

import domains.usergroup.objects.*

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)
