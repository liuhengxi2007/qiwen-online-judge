import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import { fromSubmissionSummaryContract } from '@/objects/submission/response/SubmissionSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type SubmissionListResponse = PageResponse<SubmissionSummary>

export function fromSubmissionListResponseContract(
  value: unknown,
  label = 'submission list response',
): SubmissionListResponse {
  return fromPageResponseContract(value, label, fromSubmissionSummaryContract)
}
