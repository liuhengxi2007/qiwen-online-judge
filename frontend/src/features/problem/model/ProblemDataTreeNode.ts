import type { ProblemDataPath } from '@/features/problem/model/ProblemDataPath'
import type { ProblemDataTreeNodeKind } from '@/features/problem/model/ProblemDataTreeNodeKind'

export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}
