import type {
  ProblemDataFileListResponse as ProblemDataFileListResponseContract,
  ProblemDetail as ProblemDetailContract,
  ProblemListResponse as ProblemListResponseContract,
} from '@contracts/problem'
import type { SuccessResponse } from '@contracts/shared'
import type {
  CreateProblemRequest,
  ProblemDataFileList,
  ProblemDataFilename,
  ProblemDetail,
  ProblemListResponse,
  ProblemSlug,
  UpdateProblemDataRequest,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  fromProblemListResponseContract,
  parseProblemDataFilename,
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

export async function updateProblemData(
  problemSlug: ProblemSlug,
  request: UpdateProblemDataRequest,
): Promise<ProblemDetail> {
  const response = await postJson<ProblemDetailContract>(
    `/api/problems/${problemSlugValue(problemSlug)}/data`,
    request,
  )
  return fromProblemDetailContract(response)
}

export async function listProblemDataFiles(problemSlug: ProblemSlug): Promise<ProblemDataFileList> {
  const response = await requestJson<ProblemDataFileListResponseContract>(
    `/api/problems/${problemSlugValue(problemSlug)}/data`,
  )
  return response.items.map((item: string, index: number) => {
    const result = parseProblemDataFilename(item)
    if (!result.ok) {
      throw new Error(`Invalid problem data filename at index ${index}: ${result.error}`)
    }
    return result.value
  })
}

export async function deleteProblemData(problemSlug: ProblemSlug, filename: ProblemDataFilename): Promise<ProblemDetail> {
  const response = await postJson<ProblemDetailContract>(
    `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}/delete`,
    {},
  )
  return fromProblemDetailContract(response)
}

export function problemDataDownloadUrl(problemSlug: ProblemSlug, filename: ProblemDataFilename): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
}
