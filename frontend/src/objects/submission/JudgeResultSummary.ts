import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 判题结果摘要；封装分数、verdict、资源消耗和失败原因。对象对齐例外：该类型镜像 judge-protocol，不对应后端 submission domain object。 */
export type JudgeResultSummary = {
  score: number
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
