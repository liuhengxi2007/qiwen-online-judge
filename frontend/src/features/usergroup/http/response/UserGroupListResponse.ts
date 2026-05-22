import type { UserGroupSummary } from '@/features/usergroup/http/response/UserGroupSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type UserGroupListResponse = PageResponse<UserGroupSummary>
