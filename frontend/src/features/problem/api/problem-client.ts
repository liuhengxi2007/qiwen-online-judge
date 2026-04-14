import type { SuccessResponse } from '@contracts/shared'
import type {
  CreateProblemRequest,
  ProblemDataFileListResponse,
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
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'

export async function listProblems(): Promise<ProblemListResponse> {
  return requestJson('/api/problems', fromProblemListResponseContract)
}

export async function createProblem(request: CreateProblemRequest): Promise<ProblemDetail> {
  return postJson('/api/problems', fromProblemDetailContract, toCreateProblemRequestContract(request))
}

export async function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}`, fromProblemDetailContract)
}

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    fromProblemDetailContract,
    toUpdateProblemRequestContract(request),
  )
}

export function deleteProblem(problemSlug: ProblemSlug): Promise<SuccessResponse> {
  return postJson(`/api/problems/${problemSlugValue(problemSlug)}/delete`, decodeSuccessResponse, {})
}

export async function updateProblemData(
  problemSlug: ProblemSlug,
  request: UpdateProblemDataRequest,
): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data`,
    fromProblemDetailContract,
    request,
  )
}

export async function listProblemDataFiles(problemSlug: ProblemSlug): Promise<ProblemDataFileListResponse> {
  return requestJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data`,
    (value) => {
      if (typeof value !== 'object' || value === null || !('items' in value) || !Array.isArray(value.items)) {
        throw new Error('Invalid problem data file list payload.')
      }

      const items = value.items.map((item: unknown, index: number) => {
        if (typeof item !== 'string') {
          throw new Error(`Invalid problem data filename at index ${index}: expected string.`)
        }

        const result = parseProblemDataFilename(item)
        if (!result.ok) {
          throw new Error(`Invalid problem data filename at index ${index}: ${result.error}`)
        }
        return result.value
      })

      return { items }
    },
  )
}

export async function deleteProblemData(problemSlug: ProblemSlug, filename: ProblemDataFilename): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}/delete`,
    fromProblemDetailContract,
    {},
  )
}

export async function clearProblemData(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/clear`,
    fromProblemDetailContract,
    {},
  )
}

export function problemDataDownloadUrl(problemSlug: ProblemSlug, filename: ProblemDataFilename): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
}
