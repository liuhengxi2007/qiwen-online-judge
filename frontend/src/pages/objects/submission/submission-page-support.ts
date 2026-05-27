import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import {
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionVerdictFilter,
  parseSubmissionProblemQuery,
  parseSubmissionUserQuery,
  submissionVerdictLabel,
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

export type { ProblemSuggestion, SubmissionSort, SubmissionSortDirection, SubmissionSummary, SubmissionVerdictFilter }

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

export function readSubmissionSort(searchParams: URLSearchParams): SubmissionSort {
  const rawSort = searchParams.get('sort')
  return rawSort && isSubmissionSort(rawSort) ? rawSort : 'submitted'
}

export function readSubmissionSortDirection(
  searchParams: URLSearchParams,
  activeSort: SubmissionSort,
): SubmissionSortDirection {
  const rawDirection = searchParams.get('direction')
  return rawDirection && isSubmissionSortDirection(rawDirection) ? rawDirection : defaultSortDirection(activeSort)
}

export function readSubmissionVerdictFilter(searchParams: URLSearchParams): SubmissionVerdictFilter {
  const rawVerdict = searchParams.get('verdict')
  return rawVerdict && isSubmissionVerdictFilter(rawVerdict) ? rawVerdict : 'all'
}

export function shouldShowTypingSuggestions(value: string): boolean {
  return value.trim().length > 0
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

export function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
  if (verdict === 'all') {
    return allVerdictsLabel
  }
  if (verdict === 'pending') {
    return submissionVerdictLabel(null)
  }
  return submissionVerdictLabel(verdict)
}
