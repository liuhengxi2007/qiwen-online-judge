package domains.user.application.input


import shared.model.PageRequest

final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)
