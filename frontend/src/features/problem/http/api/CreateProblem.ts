import type { CreateProblemRequest } from '@/features/problem/model/request/CreateProblemRequest'
import type { ProblemDetail } from '@/features/problem/model/response/ProblemDetail'
import {
  fromProblemDetailContract,
  toCreateProblemRequestContract,
} from '@/features/problem/http/codec/ProblemHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson('/api/problems', fromProblemDetailContract, toCreateProblemRequestContract(request))
}
