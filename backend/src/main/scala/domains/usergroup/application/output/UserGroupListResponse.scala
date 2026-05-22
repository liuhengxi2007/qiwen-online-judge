package domains.usergroup.application.output

import domains.usergroup.model.*

import domains.shared.model.PageResponse

type UserGroupListResponse = PageResponse[UserGroupSummary]
