import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeResult = {
  score: number
  lowestScore: number
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  subtasks: JudgeSubtaskResult[]
  baseResult: JudgeResult | null
}
