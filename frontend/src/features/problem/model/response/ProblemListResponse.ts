import type { ProblemSummary } from '@/features/problem/model/response/ProblemSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type ProblemListResponse = PageResponse<ProblemSummary>
