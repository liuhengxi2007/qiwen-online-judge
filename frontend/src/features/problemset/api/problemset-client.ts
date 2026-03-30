import type {
  AddProblemToProblemSetRequest,
  CreateProblemSetRequest,
  ProblemSetDetail,
  ProblemSetListResponse,
  ProblemSetSlug,
  ProblemSetSummary,
} from '@/features/problemset/domain/problemset'
import { postJson, requestJson } from '@/shared/api/http-client'
import { problemSetSlugValue } from '@/features/problemset/domain/problemset'

export function listProblemSets(): Promise<ProblemSetListResponse> {
  return requestJson<ProblemSetListResponse>('/api/problem-sets')
}

export function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  return postJson<ProblemSetSummary>('/api/problem-sets', request)
}

export function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  return requestJson<ProblemSetDetail>(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`)
}

export function addProblemToProblemSet(
  problemSetSlug: ProblemSetSlug,
  request: AddProblemToProblemSetRequest,
): Promise<ProblemSetDetail> {
  return postJson<ProblemSetDetail>(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems`, request)
}
