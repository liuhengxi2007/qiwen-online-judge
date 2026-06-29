import type { PageResponse } from '@/objects/shared/PageResponse'
import type { HackSummary } from '@/objects/hack/response/HackSummary'

/** Hack 列表分页响应；条目为摘要对象。 */
export type HackListResponse = PageResponse<HackSummary>
