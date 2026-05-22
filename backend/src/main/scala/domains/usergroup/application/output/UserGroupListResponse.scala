package domains.usergroup.application.output

import domains.usergroup.model.*

import shared.model.PageResponse

type UserGroupListResponse = PageResponse[UserGroupSummary]
