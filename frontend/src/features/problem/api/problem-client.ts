import type { CreateProblemRequest, ProblemDetail, ProblemListResponse, ProblemSlug } from '@/features/problem/domain/problem'
import { postJson, requestJson } from '@/shared/api/http-client'
import { problemSlugValue } from '@/features/problem/domain/problem'

export function listProblems(): Promise<ProblemListResponse> {
  return requestJson<ProblemListResponse>('/api/problems')
}

export function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson<ProblemDetail>('/api/problems', request)
}

export function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return requestJson<ProblemDetail>(`/api/problems/${problemSlugValue(problemSlug)}`)
}
