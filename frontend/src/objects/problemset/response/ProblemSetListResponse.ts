import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'
import { fromProblemSetSummaryContract } from '@/objects/problemset/response/ProblemSetSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>

export function fromProblemSetListResponseContract(
  value: unknown,
  label = 'problem set list response',
): ProblemSetListResponse {
  return fromPageResponseContract(value, label, fromProblemSetSummaryContract)
}
