import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'

export type JudgeResult = {
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  subtasks: JudgeSubtaskResult[]
}
