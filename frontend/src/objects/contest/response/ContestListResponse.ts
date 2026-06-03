import type { ContestSummary } from '@/objects/contest/response/ContestSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type ContestListResponse = PageResponse<ContestSummary>
