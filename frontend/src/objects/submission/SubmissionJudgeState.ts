import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type SubmissionJudgeState = {
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  score: number | null
  judgeResult: JudgeResult | null
  startedAt: string | null
  finishedAt: string | null
}
