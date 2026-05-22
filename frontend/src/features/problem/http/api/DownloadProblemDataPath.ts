import type {
  ProblemDataPath,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  problemDataPathValue,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { HttpClientError } from '@/shared/api/http-client'

export async function readProblemDataText(problemSlug: ProblemSlug, path: ProblemDataPath): Promise<string> {
  const response = await fetch(problemDataPathDownloadUrl(problemSlug, path), {
    credentials: 'same-origin',
  })

  if (!response.ok) {
    throw new HttpClientError(
      response.status === 404 ? 'not-found' : response.status === 403 ? 'forbidden' : response.status === 401 ? 'unauthorized' : 'http',
      response.statusText || `Unable to read ${problemDataPathValue(path)}.`,
    )
  }

  return response.text()
}

export function problemDataPathDownloadUrl(problemSlug: ProblemSlug, path: ProblemDataPath): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/file?path=${encodeURIComponent(problemDataPathValue(path))}`
}
