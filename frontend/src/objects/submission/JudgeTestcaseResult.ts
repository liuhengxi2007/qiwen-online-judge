import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 单个测试点判题结果；message 可能包含 checker 或系统返回的说明。对象对齐例外：该类型镜像 judge-protocol，不对应后端 submission domain object。 */
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
