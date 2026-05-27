package domains.usergroup.objects.request

import domains.usergroup.objects.*

final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)
