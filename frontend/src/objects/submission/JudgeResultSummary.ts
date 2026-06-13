import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 判题结果摘要；封装分数、verdict、资源消耗和失败原因。 */
export type JudgeResultSummary = {
  score: number
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
