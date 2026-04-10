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
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'

export async function listProblemSets(): Promise<ProblemSetListResponse> {
  return requestJson('/api/problem-sets', fromProblemSetListResponseContract)
}

export async function createProblemSet(request: CreateProblemSetRequest): Promise<ProblemSetSummary> {
  return postJson('/api/problem-sets', fromProblemSetSummaryContract, toCreateProblemSetRequestContract(request))
}

export async function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  return requestJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`, fromProblemSetDetailContract)
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
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems`,
    fromProblemSetDetailContract,
    toLinkProblemRequestContract(request),
  )
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
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`,
    fromProblemSetDetailContract,
    toUpdateProblemSetRequestContract(request),
  )
}

export function deleteProblemSet(problemSetSlug: ProblemSetSlug): Promise<SuccessResponse> {
  return postJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`, decodeSuccessResponse, {})
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
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems/${problemSlug}/remove`,
    fromProblemSetDetailContract,
    {},
  )
}
