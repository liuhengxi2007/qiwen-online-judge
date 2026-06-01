import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import { fromProblemDataPathContract } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNodeKind } from '@/objects/problem/response/ProblemDataTreeNodeKind'
import { fromProblemDataTreeNodeKindContract } from '@/objects/problem/response/ProblemDataTreeNodeKind'
import { readNonNegativeSafeInteger, readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}

export function fromProblemDataTreeNodeContract(value: unknown, label: string): ProblemDataTreeNode {
  const node = readRecord(value, label)
  return {
    path: fromProblemDataPathContract(readString(node.path, `${label} path`), `${label} path`),
    kind: fromProblemDataTreeNodeKindContract(node.kind),
    sizeBytes: readNullable(node.sizeBytes, `${label} size bytes`, readNonNegativeSafeInteger),
  }
}
