import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/** 用户已通过题目记录；用于个人主页和 accepted 排行统计。 */
export type UserAcceptedProblem = {
  slug: ProblemSlug
  title: ProblemTitle
  acceptedAt: string
}
