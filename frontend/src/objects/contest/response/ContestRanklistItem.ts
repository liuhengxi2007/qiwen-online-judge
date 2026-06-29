import type { ContestPenaltyMillis } from '@/objects/contest/ContestPenaltyMillis'
import type { ContestRank } from '@/objects/contest/ContestRank'
import type { ContestScore } from '@/objects/contest/ContestScore'
import type { ContestRanklistProblemResult } from '@/objects/contest/response/ContestRanklistProblemResult'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 比赛排行榜条目；包含总分、罚时和每题结果。 */
export type ContestRanklistItem = {
  rank: ContestRank
  user: UserIdentity
  totalScore: ContestScore
  penaltyMillis: ContestPenaltyMillis
  problemResults: ContestRanklistProblemResult[]
}
