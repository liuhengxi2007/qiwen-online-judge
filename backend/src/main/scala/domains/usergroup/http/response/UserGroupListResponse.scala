package domains.usergroup.http.response

import domains.usergroup.model.*

import domains.shared.model.PageResponse

type UserGroupListResponse = PageResponse[UserGroupSummary]
