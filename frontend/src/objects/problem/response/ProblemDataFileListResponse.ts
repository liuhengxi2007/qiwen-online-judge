import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import { fromProblemDataFilenameContract } from '@/objects/problem/ProblemDataFilename'
import { readArray, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemDataFileListResponse = {
  items: ProblemDataFilename[]
}

export function fromProblemDataFileListResponseContract(
  value: unknown,
  label = 'problem data file list response',
): ProblemDataFileListResponse {
  const response = readRecord(value, label)
  return {
    items: readArray(response.items, `${label} items`, (item, itemLabel) =>
      fromProblemDataFilenameContract(readString(item, itemLabel), itemLabel),
    ),
  }
}
