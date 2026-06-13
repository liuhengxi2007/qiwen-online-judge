import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

/** 向比赛加入题目的请求体；目标比赛由 API path 指定。 */
export type AddProblemToContestRequest = {
  problemSlug: ProblemSlug
}
