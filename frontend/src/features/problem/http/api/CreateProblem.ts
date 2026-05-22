import type {
  CreateProblemRequest,
  ProblemDetail,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  toCreateProblemRequestContract,
} from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson('/api/problems', fromProblemDetailContract, toCreateProblemRequestContract(request))
}
