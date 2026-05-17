import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type JudgeTestcaseResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  message: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}

export type JudgeSubtaskResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  testcases: JudgeTestcaseResult[]
}

export type JudgeResult = {
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  subtasks: JudgeSubtaskResult[]
}
