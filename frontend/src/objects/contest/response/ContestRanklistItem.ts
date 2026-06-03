import type { ContestPenaltyMillis } from '@/objects/contest/ContestPenaltyMillis'
import type { ContestRank } from '@/objects/contest/ContestRank'
import type { ContestScore } from '@/objects/contest/ContestScore'
import type { UserIdentity } from '@/objects/user/UserIdentity'

export type ContestRanklistItem = {
  rank: ContestRank
  user: UserIdentity
  totalScore: ContestScore
  penaltyMillis: ContestPenaltyMillis
}
