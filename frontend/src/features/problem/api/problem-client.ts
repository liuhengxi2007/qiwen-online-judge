import type {
  ProblemDetail as ProblemDetailContract,
  ProblemListResponse as ProblemListResponseContract,
} from '@contracts/problem'
import type { SuccessResponse } from '@contracts/shared'
import type {
  CreateProblemRequest,
  ProblemDetail,
  ProblemListResponse,
  ProblemSlug,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  fromProblemListResponseContract,
  problemSlugValue,
  toCreateProblemRequestContract,
  toUpdateProblemRequestContract,
} from '@/features/problem/domain/problem'
import { postJson, requestJson } from '@/shared/api/http-client'

export async function listProblems(): Promise<ProblemListResponse> {
  const response = await requestJson<ProblemListResponseContract>('/api/problems')
  return fromProblemListResponseContract(response)
}

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  const response = await postJson<ProblemDetailContract>('/api/problems', toCreateProblemRequestContract(request))
  return fromProblemDetailContract(response)
}

export async function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  const response = await requestJson<ProblemDetailContract>(`/api/problems/${problemSlugValue(problemSlug)}`)
  return fromProblemDetailContract(response)
}

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  const response = await postJson<ProblemDetailContract>(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    toUpdateProblemRequestContract(request),
  )
  return fromProblemDetailContract(response)
}

export function deleteProblem(problemSlug: ProblemSlug): Promise<SuccessResponse> {
  return postJson<SuccessResponse>(`/api/problems/${problemSlugValue(problemSlug)}/delete`, {})
}
