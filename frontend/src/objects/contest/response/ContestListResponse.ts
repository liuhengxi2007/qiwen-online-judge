import type { ContestSummary } from '@/objects/contest/response/ContestSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 比赛列表分页响应；条目为摘要对象。 */
export type ContestListResponse = PageResponse<ContestSummary>
