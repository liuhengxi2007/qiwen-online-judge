import type { UserGroupSummary } from '@/objects/usergroup/response/UserGroupSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 用户组列表分页响应；条目为摘要对象。 */
export type UserGroupListResponse = PageResponse<UserGroupSummary>
