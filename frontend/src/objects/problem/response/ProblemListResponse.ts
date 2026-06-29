import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 题目列表分页响应；条目为不含题面的摘要对象。 */
export type ProblemListResponse = PageResponse<ProblemSummary>
