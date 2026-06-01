import type { UserGroupSummary } from '@/objects/usergroup/response/UserGroupSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type UserGroupListResponse = PageResponse<UserGroupSummary>