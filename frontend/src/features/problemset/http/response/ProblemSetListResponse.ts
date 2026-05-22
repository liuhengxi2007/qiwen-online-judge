import type { ProblemSetSummary } from '@/features/problemset/http/response/ProblemSetSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
