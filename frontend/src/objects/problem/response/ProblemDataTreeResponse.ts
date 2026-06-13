import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

/** 题目数据树响应；items 为后端整理后的扁平节点集合。 */
export type ProblemDataTreeResponse = {
  items: ProblemDataTreeNode[]
}
