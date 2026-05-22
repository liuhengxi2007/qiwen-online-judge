import type { JudgeSubtaskResult } from '@/features/submission/model/JudgeSubtaskResult'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type JudgeResult = {
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  subtasks: JudgeSubtaskResult[]
}
