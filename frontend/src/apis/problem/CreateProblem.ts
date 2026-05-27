import type { CreateProblemRequest } from '@/objects/problem/request/CreateProblemRequest'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import {
  fromProblemDetailContract,
  toCreateProblemRequestContract,
} from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson('/api/problems', fromProblemDetailContract, toCreateProblemRequestContract(request))
}
