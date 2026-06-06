import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeResultSummary = {
  score: number
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
