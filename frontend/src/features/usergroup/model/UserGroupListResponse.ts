import type { UserGroupSummary } from '@/features/usergroup/model/UserGroupSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type UserGroupListResponse = PageResponse<UserGroupSummary>
