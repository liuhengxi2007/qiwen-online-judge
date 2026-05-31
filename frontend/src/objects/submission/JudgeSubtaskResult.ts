import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeSubtaskResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  message: string | null
  testcases: JudgeTestcaseResult[]
}
