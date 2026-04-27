import type { SuccessResponse } from '@contracts/shared'
import type {
  CreateProblemRequest,
  ProblemDataFileListResponse,
  ProblemDataFilename,
  ProblemDetail,
  ProblemListRequest,
  ProblemListResponse,
  ProblemSlug,
  ProblemSuggestion,
  UpdateProblemDataRequest,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  fromProblemListResponseContract,
  fromProblemSuggestionContract,
  parseProblemDataFilename,
  parseProblemSearchQuery,
  problemSlugValue,
  toProblemListRequestContract,
  toCreateProblemRequestContract,
  toUpdateProblemRequestContract,
} from '@/features/problem/domain/problem'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'

export async function listProblems(request: ProblemListRequest): Promise<ProblemListResponse> {
  const url = new URL('/api/problems', window.location.origin)
  const contractRequest = toProblemListRequestContract(request)
  if (contractRequest.query !== null && contractRequest.query.trim()) {
    url.searchParams.set('q', contractRequest.query)
  }
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))
  return requestJson(url.pathname + url.search, fromProblemListResponseContract)
}

export async function listProblemSuggestions(query: string): Promise<ProblemSuggestion[]> {
  const parsedQuery = parseProblemSearchQuery(query)
  if (!parsedQuery.ok) {
    return []
  }

  const url = new URL('/api/problems/suggestions', window.location.origin)
  url.searchParams.set('q', parsedQuery.value)
  return requestJson(url.pathname + url.search, (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid problem suggestion payload.')
    }

    return value.map(fromProblemSuggestionContract)
  })
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
