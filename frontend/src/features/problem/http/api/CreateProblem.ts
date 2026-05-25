import type { CreateProblemRequest } from '@/features/problem/http/request/CreateProblemRequest'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import {
  fromProblemDetailContract,
  toCreateProblemRequestContract,
} from '@/features/problem/http/codec/ProblemHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson('/api/problems', fromProblemDetailContract, toCreateProblemRequestContract(request))
}
