import type {
  CreateProblemSetRequest,
  ProblemSetSummary,
} from '@/features/problemset/domain/problemset'
import {
  fromProblemSetSummaryContract,
  toCreateProblemSetRequestContract,
} from '@/features/problemset/domain/problemset'
import { postJson } from '@/shared/api/http-client'

export async function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  return postJson('/api/problem-sets', fromProblemSetSummaryContract, toCreateProblemSetRequestContract(request))
}
