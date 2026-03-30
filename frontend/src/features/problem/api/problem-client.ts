import type {
  CreateProblemRequest,
  ProblemDetail,
  ProblemListResponse,
  ProblemSlug,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
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

export function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson<ProblemDetail>(`/api/problems/${problemSlugValue(problemSlug)}`, request)
}

export function deleteProblem(problemSlug: ProblemSlug): Promise<{ message: string }> {
  return postJson<{ message: string }>(`/api/problems/${problemSlugValue(problemSlug)}/delete`, {})
}
