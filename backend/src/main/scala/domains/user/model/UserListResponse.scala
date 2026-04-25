package domains.user.model

import domains.shared.model.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
