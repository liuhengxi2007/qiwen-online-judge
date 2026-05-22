package domains.user.application.input

import domains.user.model.*

import shared.model.PageRequest

final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)
