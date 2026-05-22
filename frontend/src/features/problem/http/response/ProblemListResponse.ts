import type { ProblemSummary } from '@/features/problem/http/response/ProblemSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type ProblemListResponse = PageResponse<ProblemSummary>
