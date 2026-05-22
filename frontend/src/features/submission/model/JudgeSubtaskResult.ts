import type { JudgeTestcaseResult } from '@/features/submission/model/JudgeTestcaseResult'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type JudgeSubtaskResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  testcases: JudgeTestcaseResult[]
}
