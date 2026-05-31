import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemDataManifestEntry } from '@/objects/problem/ProblemDataManifestEntry'

export type ProblemDataManifest = {
  problemSlug: ProblemSlug
  entries: ProblemDataManifestEntry[]
  version: string
}
