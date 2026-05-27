import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type ProblemListResponse = PageResponse<ProblemSummary>
