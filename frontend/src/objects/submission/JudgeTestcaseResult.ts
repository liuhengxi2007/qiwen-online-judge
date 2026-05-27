import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type JudgeTestcaseResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  message: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}
