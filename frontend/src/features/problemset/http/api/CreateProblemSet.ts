import type { CreateProblemSetRequest } from '@/features/problemset/http/request/CreateProblemSetRequest'
import type { ProblemSetSummary } from '@/features/problemset/http/response/ProblemSetSummary'
import {
  fromProblemSetSummaryContract,
  toCreateProblemSetRequestContract,
} from '@/features/problemset/http/codec/ProblemSetHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  return postJson('/api/problem-sets', fromProblemSetSummaryContract, toCreateProblemSetRequestContract(request))
}
