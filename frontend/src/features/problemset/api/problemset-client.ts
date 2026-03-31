import type {
  ProblemSetDetail as ProblemSetDetailContract,
  ProblemSetListResponse as ProblemSetListResponseContract,
  ProblemSetSummary as ProblemSetSummaryContract,
} from '@contracts/problemset'
import type { SuccessResponse } from '@contracts/shared'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import type {
  AddProblemToProblemSetRequest,
  CreateProblemSetRequest,
  ProblemSetDetail,
  ProblemSetListResponse,
  ProblemSetSlug,
  ProblemSetSummary,
  UpdateProblemSetRequest,
} from '@/features/problemset/domain/problemset'
import {
  fromProblemSetDetailContract,
  fromProblemSetListResponseContract,
  fromProblemSetSummaryContract,
  problemSetSlugValue,
  toCreateProblemSetRequestContract,
  toLinkProblemRequestContract,
  toUpdateProblemSetRequestContract,
} from '@/features/problemset/domain/problemset'
import { postJson, requestJson } from '@/shared/api/http-client'

export async function listProblemSets(): Promise<ProblemSetListResponse> {
  const response = await requestJson<ProblemSetListResponseContract>('/api/problem-sets')
  return fromProblemSetListResponseContract(response)
}

export async function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  const response = await postJson<ProblemSetSummaryContract>('/api/problem-sets', toCreateProblemSetRequestContract(request))
  return fromProblemSetSummaryContract(response)
}

export async function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  const response = await requestJson<ProblemSetDetailContract>(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`)
  return fromProblemSetDetailContract(response)
}

export function addProblemToProblemSet(
  problemSetSlug: ProblemSetSlug,
  request: AddProblemToProblemSetRequest,
): Promise<ProblemSetDetail> {
  return addProblemToProblemSetInternal(problemSetSlug, request)
}

async function addProblemToProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  request: AddProblemToProblemSetRequest,
): Promise<ProblemSetDetail> {
  const response = await postJson<ProblemSetDetailContract>(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems`,
    toLinkProblemRequestContract(request),
  )
  return fromProblemSetDetailContract(response)
}

export function updateProblemSet(
  problemSetSlug: ProblemSetSlug,
  request: UpdateProblemSetRequest,
): Promise<ProblemSetDetail> {
  return updateProblemSetInternal(problemSetSlug, request)
}

async function updateProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  request: UpdateProblemSetRequest,
): Promise<ProblemSetDetail> {
  const response = await postJson<ProblemSetDetailContract>(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`,
    toUpdateProblemSetRequestContract(request),
  )
  return fromProblemSetDetailContract(response)
}

export function deleteProblemSet(problemSetSlug: ProblemSetSlug): Promise<SuccessResponse> {
  return postJson<SuccessResponse>(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`, {})
}

export function removeProblemFromProblemSet(
  problemSetSlug: ProblemSetSlug,
  problemSlug: ProblemSlug,
): Promise<ProblemSetDetail> {
  return removeProblemFromProblemSetInternal(problemSetSlug, problemSlug)
}

async function removeProblemFromProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  problemSlug: ProblemSlug,
): Promise<ProblemSetDetail> {
  const response = await postJson<ProblemSetDetailContract>(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems/${problemSlug}/remove`,
    {},
  )
  return fromProblemSetDetailContract(response)
}
