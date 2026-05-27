import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type SubmissionListResponse = PageResponse<SubmissionSummary>
