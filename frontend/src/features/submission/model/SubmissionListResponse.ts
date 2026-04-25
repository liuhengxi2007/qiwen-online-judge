import type { SubmissionSummary } from '@/features/submission/model/SubmissionSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type SubmissionListResponse = PageResponse<SubmissionSummary>
