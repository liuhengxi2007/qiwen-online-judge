import type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type SubmissionJudgeState = {
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
  startedAt: string | null
  finishedAt: string | null
}
