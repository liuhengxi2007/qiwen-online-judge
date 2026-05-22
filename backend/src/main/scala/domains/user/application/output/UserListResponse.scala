package domains.user.application.output

import domains.user.model.*

import domains.shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
