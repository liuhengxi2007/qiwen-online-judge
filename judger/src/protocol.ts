export type SubmissionLanguage = 'cpp17' | 'python3'
export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'
export type SubmissionVerdict =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'

export type ClaimJudgeTaskRequest = {
  judgerName: string
}

export type JudgeTaskTestcase = {
  name: string
  inputBase64: string
  expectedOutputBase64: string
}

export type JudgeTask = {
  submissionId: number
  problemSlug: string
  language: SubmissionLanguage
  sourceCode: string
  timeLimitMs: number
  spaceLimitMb: number
  testcases: JudgeTaskTestcase[]
}

export type ReportJudgeResultRequest = {
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
}
