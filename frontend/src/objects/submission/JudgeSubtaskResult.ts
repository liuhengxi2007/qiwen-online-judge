import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'

/** 子任务判题结果；包含子任务摘要和各测试点结果。 */
export type JudgeSubtaskResult = {
  index: number
  label: string | null
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  testcases: JudgeTestcaseResult[]
}
