import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export function problemDataDownloadUrl(problemSlug: ProblemSlug, filename: ProblemDataFilename): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
}
