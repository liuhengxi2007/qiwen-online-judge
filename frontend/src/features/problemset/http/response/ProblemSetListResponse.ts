import type { ProblemSetSummary } from '@/features/problemset/http/response/ProblemSetSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type ProblemSetListResponse = PageResponse<ProblemSetSummary>
