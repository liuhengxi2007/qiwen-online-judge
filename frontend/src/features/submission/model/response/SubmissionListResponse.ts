import type { SubmissionSummary } from '@/features/submission/model/response/SubmissionSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type SubmissionListResponse = PageResponse<SubmissionSummary>
