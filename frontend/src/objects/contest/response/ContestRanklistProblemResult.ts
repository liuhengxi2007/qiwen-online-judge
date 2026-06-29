import type { ContestProblemSummary } from '@/objects/contest/ContestProblemSummary'
import type { ContestPenaltyMillis } from '@/objects/contest/ContestPenaltyMillis'
import type { ContestScore } from '@/objects/contest/ContestScore'
import type { SubmissionId } from '@/objects/submission/SubmissionId'

/** 比赛排行榜单题结果；canViewDetail 控制是否可跳转查看提交详情。 */
export type ContestRanklistProblemResult = {
  problem: ContestProblemSummary
  score: ContestScore | null
  penaltyMillis: ContestPenaltyMillis | null
  submittedAt: string | null
  submissionId: SubmissionId | null
  canViewDetail: boolean
}
