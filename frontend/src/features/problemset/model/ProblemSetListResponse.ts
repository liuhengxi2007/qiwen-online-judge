import type { ProblemSetSummary } from '@/features/problemset/model/ProblemSetSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
