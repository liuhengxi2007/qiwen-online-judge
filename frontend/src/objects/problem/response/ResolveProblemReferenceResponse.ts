import type { ProblemReference } from '@/objects/problem/ProblemReference'

/** 题目引用解析响应；problem 为空表示 slug 未解析到可用题目。 */
export type ResolveProblemReferenceResponse = {
  problem: ProblemReference | null
}
