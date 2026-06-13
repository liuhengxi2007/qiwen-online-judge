import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 单个测试点判题结果；message 可能包含 checker 或系统返回的说明。 */
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
