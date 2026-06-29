import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

/** 向题集追加题目的请求体；目标题集由 API path 指定。 */
export type AddProblemToProblemSetRequest = {
  problemSlug: ProblemSlug
}
