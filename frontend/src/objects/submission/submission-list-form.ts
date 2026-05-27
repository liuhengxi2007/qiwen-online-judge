import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import {
  parseSubmissionProblemQuery,
  parseSubmissionUserQuery,
} from '@/objects/submission/submission-parsers'

export const submissionsPerPage = 10

export const verdictFilterValues = [
  'all',
  'pending',
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

export const submissionSortValues = [
  'submitted',
  'time',
  'memory',
  'code_length',
] as const satisfies readonly SubmissionSort[]

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
