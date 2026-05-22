import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type JudgeTestcaseResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  message: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
