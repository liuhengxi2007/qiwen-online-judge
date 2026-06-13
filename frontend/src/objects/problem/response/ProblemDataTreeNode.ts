import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNodeKind } from '@/objects/problem/response/ProblemDataTreeNodeKind'

/** 题目数据树节点；目录 sizeBytes 为空，文件节点携带字节大小。 */
export type ProblemDataTreeNode = {
  path: ProblemDataPath
  kind: ProblemDataTreeNodeKind
  sizeBytes: number | null
}
