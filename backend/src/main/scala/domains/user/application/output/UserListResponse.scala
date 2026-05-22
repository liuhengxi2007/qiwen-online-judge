package domains.user.application.output

import domains.user.model.*

import shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
