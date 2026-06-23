import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'

/** 子任务判题结果；包含子任务摘要和各测试点结果。对象对齐例外：该类型镜像 judge-protocol，不对应后端 submission domain object。 */
export type JudgeSubtaskResult = {
  index: number
  label: string | null
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  testcases: JudgeTestcaseResult[]
}
