import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'
import { fromProblemDataTreeNodeContract } from '@/objects/problem/response/ProblemDataTreeNode'
import { readArray, readRecord } from '@/objects/shared/PageResponse'

export type ProblemDataTreeResponse = {
  items: ProblemDataTreeNode[]
}

export function fromProblemDataTreeResponseContract(value: unknown, label = 'problem data tree response'): ProblemDataTreeResponse {
  const response = readRecord(value, label)
  return {
    items: readArray(response.items, `${label} items`, fromProblemDataTreeNodeContract),
  }
}
