import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 题目列表请求；可选搜索词和分页参数会由 API class 编码成查询串。 */
export type ProblemListRequest = {
  query: ProblemSearchQuery | null
  pageRequest: PageRequest
}
