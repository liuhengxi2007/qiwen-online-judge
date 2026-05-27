import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'
import {
  fromProblemSetSummaryContract,
  toCreateProblemSetRequestContract,
} from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  return postJson('/api/problem-sets', fromProblemSetSummaryContract, toCreateProblemSetRequestContract(request))
}
