package domains.user.model.request


import shared.model.PageRequest

final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)
