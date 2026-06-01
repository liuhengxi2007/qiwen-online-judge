import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemDataManifestEntry } from '@/objects/problem/ProblemDataManifestEntry'
import { fromProblemDataManifestEntryContract } from '@/objects/problem/ProblemDataManifestEntry'
import { readArray, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemDataManifest = {
  problemSlug: ProblemSlug
  entries: ProblemDataManifestEntry[]
  version: string
}

export function fromProblemDataManifestContract(value: unknown, label = 'problem data manifest'): ProblemDataManifest {
  const manifest = readRecord(value, label)
  return {
    problemSlug: fromProblemSlugContract(readString(manifest.problemSlug, `${label} problem slug`), `${label} problem slug`),
    entries: readArray(manifest.entries, `${label} entries`, fromProblemDataManifestEntryContract),
    version: readString(manifest.version, `${label} version`),
  }
}
