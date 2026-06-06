import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeTestcaseResult = {
  index: number
  label: string | null
  testcaseType: 'main' | 'sample' | 'hack'
  score: number
  verdict: SubmissionVerdict
  message: string | null
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
