import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNodeKind } from '@/objects/problem/ProblemDataTreeNodeKind'

export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}
