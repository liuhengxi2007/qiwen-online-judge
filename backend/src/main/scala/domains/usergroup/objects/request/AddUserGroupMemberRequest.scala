package domains.usergroup.objects.request

import domains.usergroup.objects.*

import domains.user.objects.Username

final case class AddUserGroupMemberRequest(
  username: Username,
  role: AddUserGroupMemberRole
)
