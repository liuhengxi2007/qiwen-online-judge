import type { SubmissionSummary } from '@/features/submission/http/response/SubmissionSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type SubmissionListResponse = PageResponse<SubmissionSummary>
