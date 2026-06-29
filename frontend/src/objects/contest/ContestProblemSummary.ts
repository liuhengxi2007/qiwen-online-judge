import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ContestProblemAlias } from '@/objects/contest/ContestProblemAlias'

/** 比赛题目摘要；position 和 alias 决定比赛内展示顺序与题号。 */
export type ContestProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: number
  alias: ContestProblemAlias
}
