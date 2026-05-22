package domains.user.http.response

import domains.user.model.*

import domains.shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
