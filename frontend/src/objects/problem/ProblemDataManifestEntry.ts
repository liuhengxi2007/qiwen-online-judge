import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'

export type ProblemDataManifestEntry = {
  path: ProblemDataPath
  sizeBytes: number
  sha256: string
}