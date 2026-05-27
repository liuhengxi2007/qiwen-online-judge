import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import type { SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import { verdictFilterLabel } from '@/objects/submission/submission-display'
import {
  buildSubmissionListRequest,
  defaultSortDirection,
  submissionSortValues,
  verdictFilterValues,
} from '@/objects/submission/submission-list-form'
import {
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionVerdictFilter,
} from '@/objects/submission/submission-parsers'

export {
  buildSubmissionListRequest,
  defaultSortDirection,
  submissionSortValues,
  verdictFilterLabel,
  verdictFilterValues,
}
export type { ProblemSuggestion, SubmissionSort, SubmissionSortDirection, SubmissionSummary, SubmissionVerdictFilter }

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
