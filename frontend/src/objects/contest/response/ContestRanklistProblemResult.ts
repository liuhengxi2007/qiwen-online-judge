import type { ContestProblemSummary } from '@/objects/contest/ContestProblemSummary'
import type { ContestPenaltyMillis } from '@/objects/contest/ContestPenaltyMillis'
import type { ContestScore } from '@/objects/contest/ContestScore'
import type { SubmissionId } from '@/objects/submission/SubmissionId'

export type ContestRanklistProblemResult = {
  problem: ContestProblemSummary
  score: ContestScore | null
  penaltyMillis: ContestPenaltyMillis | null
  submittedAt: string | null
  submissionId: SubmissionId | null
  canViewDetail: boolean
}
