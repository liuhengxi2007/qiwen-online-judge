package domains.usergroup.model.request

import domains.usergroup.model.*

import domains.user.model.Username

final case class AddUserGroupMemberRequest(
  username: Username,
  role: AddUserGroupMemberRole
)
