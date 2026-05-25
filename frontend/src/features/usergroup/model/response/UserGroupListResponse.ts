import type { UserGroupSummary } from '@/features/usergroup/model/response/UserGroupSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type UserGroupListResponse = PageResponse<UserGroupSummary>
