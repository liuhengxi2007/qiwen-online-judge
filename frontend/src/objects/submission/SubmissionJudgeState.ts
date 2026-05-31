import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'

export type SubmissionJudgeState = {
  status: SubmissionStatus
  judgeResult: JudgeResult | null
  startedAt: string | null
  finishedAt: string | null
}
