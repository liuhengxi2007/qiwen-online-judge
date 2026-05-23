import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemDataTreeResponse } from '@/features/problem/http/response/ProblemDataTreeResponse'
import { parseProblemDataPath, problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { requestJson } from '@/shared/api/http-client'

export async function listProblemDataTree(problemSlug: ProblemSlug): Promise<ProblemDataTreeResponse> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}/data/tree`, (value) => {
    if (typeof value !== 'object' || value === null || !('items' in value) || !Array.isArray(value.items)) {
      throw new Error('Invalid problem data tree payload.')
    }

    const items = value.items.map((item: unknown, index: number) => {
      if (typeof item !== 'object' || item === null) {
        throw new Error(`Invalid problem data tree node at index ${index}: expected object.`)
      }
      const rawPath = 'path' in item ? item.path : undefined
      const rawKind = 'kind' in item ? item.kind : undefined
      const rawSizeBytes = 'sizeBytes' in item ? item.sizeBytes : undefined
      if (typeof rawPath !== 'string') {
        throw new Error(`Invalid problem data tree node at index ${index}: expected string path.`)
      }
      if (rawKind !== 'file' && rawKind !== 'directory') {
        throw new Error(`Invalid problem data tree node at index ${index}: expected file or directory kind.`)
      }
      if (rawSizeBytes !== null && rawSizeBytes !== undefined && typeof rawSizeBytes !== 'number') {
        throw new Error(`Invalid problem data tree node at index ${index}: expected number or null sizeBytes.`)
      }
      const path = parseProblemDataPath(rawPath)
      if (!path.ok) {
        throw new Error(`Invalid problem data tree node at index ${index}: ${path.error}`)
      }
      return { path: path.value, kind: rawKind as 'file' | 'directory', sizeBytes: rawSizeBytes ?? null }
    })
    return { items }
  })
}
