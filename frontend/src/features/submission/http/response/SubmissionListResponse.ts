import type { SubmissionSummary } from '@/features/submission/http/response/SubmissionSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type SubmissionListResponse = PageResponse<SubmissionSummary>
