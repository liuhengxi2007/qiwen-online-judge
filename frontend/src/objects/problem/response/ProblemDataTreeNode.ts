import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNodeKind } from '@/objects/problem/response/ProblemDataTreeNodeKind'

export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}
