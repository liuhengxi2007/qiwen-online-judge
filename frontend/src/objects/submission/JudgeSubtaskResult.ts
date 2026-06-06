import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'

export type JudgeSubtaskResult = {
  index: number
  label: string | null
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  testcases: JudgeTestcaseResult[]
}
