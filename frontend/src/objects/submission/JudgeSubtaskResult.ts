import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { JudgeResultMetrics } from '@/objects/submission/JudgeResultMetrics'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeSubtaskResult = {
  index: number
  label: string | null
  baseResult: JudgeResultMetrics
  worstResult: JudgeResultMetrics
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  testcases: JudgeTestcaseResult[]
}
