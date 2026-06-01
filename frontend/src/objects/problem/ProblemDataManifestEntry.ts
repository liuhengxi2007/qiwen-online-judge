import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import { fromProblemDataPathContract } from '@/objects/problem/ProblemDataPath'
import { readNonNegativeSafeInteger, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemDataManifestEntry = {
  path: ProblemDataPath
  sizeBytes: number
  sha256: string
}

export function fromProblemDataManifestEntryContract(value: unknown, label: string): ProblemDataManifestEntry {
  const entry = readRecord(value, label)
  const sha256 = readString(entry.sha256, `${label} sha256`)
  if (!/^[a-f0-9]{64}$/i.test(sha256)) {
    throw new Error(`Invalid ${label} sha256 in contract payload.`)
  }

  return {
    path: fromProblemDataPathContract(readString(entry.path, `${label} path`), `${label} path`),
    sizeBytes: readNonNegativeSafeInteger(entry.sizeBytes, `${label} size bytes`),
    sha256: sha256.toLowerCase(),
  }
}
