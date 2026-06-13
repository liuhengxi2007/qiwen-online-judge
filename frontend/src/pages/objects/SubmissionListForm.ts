import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import { parseSubmissionProblemQuery } from '@/objects/submission/request/SubmissionProblemQuery'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import { parseSubmissionUserQuery } from '@/objects/submission/request/SubmissionUserQuery'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'

/**
 * 提交列表默认每页数量，供列表请求和分页 UI 保持一致。
 */
export const submissionsPerPage = 10

/**
 * 提交列表允许展示的判题结果筛选值，顺序即筛选下拉中的展示顺序。
 */
export const verdictFilterValues = [
  'all',
  'pending',
  'accepted',
  'accepted_by_protocol',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'idleness_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

/**
 * 提交列表允许的排序字段，顺序即排序下拉中的展示顺序。
 */
export const submissionSortValues = [
  'submitted',
  'time',
  'memory',
  'code_length',
] as const satisfies readonly SubmissionSort[]

/**
 * 返回指定排序字段的默认方向；提交时间默认倒序，其余资源指标默认升序。
 */
export function defaultSortDirection(sort: SubmissionSort): SubmissionSortDirection {
  switch (sort) {
    case 'submitted':
      return 'desc'
    case 'time':
    case 'memory':
    case 'code_length':
      return 'asc'
  }
}

/**
 * 根据页面筛选状态构造提交列表请求；无效用户或题目查询会被丢弃为 null。
 */
export function buildSubmissionListRequest({
  usernameQueryParam,
  activeProblemQuery,
  activeVerdictFilter,
  activeSort,
  activeDirection,
  currentPage,
}: {
  usernameQueryParam: string
  activeProblemQuery: string
  activeVerdictFilter: SubmissionVerdictFilter
  activeSort: SubmissionSort
  activeDirection: SubmissionSortDirection
  currentPage: number
}): SubmissionListRequest {
  return {
    userQuery: (() => {
      if (!usernameQueryParam) {
        return null
      }
      const parsedQuery = parseSubmissionUserQuery(usernameQueryParam)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    problemQuery: (() => {
      if (!activeProblemQuery) {
        return null
      }
      const parsedQuery = parseSubmissionProblemQuery(activeProblemQuery)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    verdict: activeVerdictFilter,
    sort: activeSort,
    direction: activeDirection,
    pageRequest: {
      page: currentPage,
      pageSize: submissionsPerPage,
    },
  }
}
