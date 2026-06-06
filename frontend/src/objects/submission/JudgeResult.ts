import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { JudgeResultMetrics } from '@/objects/submission/JudgeResultMetrics'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeResult = {
  baseResult: JudgeResultMetrics
  worstResult: JudgeResultMetrics
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  subtasks: JudgeSubtaskResult[]
}
