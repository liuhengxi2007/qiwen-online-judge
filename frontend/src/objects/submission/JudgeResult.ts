import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'

/** 完整判题结果；包含基础结果、最差结果和所有子任务明细。 */
export type JudgeResult = {
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  subtasks: JudgeSubtaskResult[]
}
