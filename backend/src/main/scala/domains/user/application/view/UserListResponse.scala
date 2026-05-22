package domains.user.application.view

import domains.user.model.*

import domains.shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
