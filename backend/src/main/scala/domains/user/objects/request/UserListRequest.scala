package domains.user.objects.request


import shared.objects.PageRequest

final case class UserListRequest(
  query: Option[UserSearchQuery],
  pageRequest: PageRequest
)
