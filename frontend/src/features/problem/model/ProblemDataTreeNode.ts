import type { ProblemDataPath } from '@/features/problem/model/ProblemDataPath'

export type ProblemDataTreeNodeKind = 'file' | 'directory'

export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}
