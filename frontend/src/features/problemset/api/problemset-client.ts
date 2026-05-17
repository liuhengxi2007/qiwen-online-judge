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
  toAddProblemToProblemSetRequestContract,
  toCreateProblemSetRequestContract,
  toUpdateProblemSetRequestContract,
} from '@/features/problemset/domain/problemset'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

export async function listProblemSets(pageRequest?: PageRequest): Promise<ProblemSetListResponse> {
  const url = new URL('/api/problem-sets', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromProblemSetListResponseContract)
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
    toAddProblemToProblemSetRequestContract(request),
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
