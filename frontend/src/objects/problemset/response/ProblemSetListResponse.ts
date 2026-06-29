import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 题集列表分页响应；条目为摘要对象。 */
export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
