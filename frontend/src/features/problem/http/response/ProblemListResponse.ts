import type { ProblemSummary } from '@/features/problem/http/response/ProblemSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type ProblemListResponse = PageResponse<ProblemSummary>
