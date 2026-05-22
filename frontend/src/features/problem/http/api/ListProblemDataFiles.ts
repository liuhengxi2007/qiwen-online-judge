import type {
  ProblemDataFileListResponse,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  parseProblemDataFilename,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { requestJson } from '@/shared/api/http-client'

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
