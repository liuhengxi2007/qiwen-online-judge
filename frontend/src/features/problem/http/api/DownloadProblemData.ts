import type {
  ProblemDataFilename,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'

export function problemDataDownloadUrl(problemSlug: ProblemSlug, filename: ProblemDataFilename): string {
  return `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
}
