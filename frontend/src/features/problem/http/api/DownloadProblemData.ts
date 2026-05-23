import type { ProblemDataFilename } from '@/features/problem/model/ProblemDataFilename'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'

export function problemDataDownloadUrl(problemSlug: ProblemSlug, filename: ProblemDataFilename): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
}
