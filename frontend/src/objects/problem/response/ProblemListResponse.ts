import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'
import { fromProblemSummaryContract } from '@/objects/problem/response/ProblemSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type ProblemListResponse = PageResponse<ProblemSummary>

export function fromProblemListResponseContract(value: unknown, label = 'problem list response'): ProblemListResponse {
  return fromPageResponseContract(value, label, fromProblemSummaryContract)
}
