import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 提交列表分页响应；条目为摘要对象。 */
export type SubmissionListResponse = PageResponse<SubmissionSummary>
