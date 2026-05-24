package domains.user.application.output


import shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
