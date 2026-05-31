import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeResult = {
  score: number
  verdict: SubmissionVerdict
  message: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  subtasks: JudgeSubtaskResult[]
}
