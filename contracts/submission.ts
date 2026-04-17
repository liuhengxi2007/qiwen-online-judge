import type { UserIdentity } from './auth'

export type SubmissionLanguage = 'cpp17' | 'python3'
export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'
export type SubmissionVerdict =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'

export type CreateSubmissionRequest = {
  problemSlug: string
  language: SubmissionLanguage
  sourceCode: string
}

export type SubmissionSummary = {
  id: number
  problemId: string
  problemSlug: string
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionDetail = {
  id: number
  problemId: string
  problemSlug: string
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
  sourceCode: string
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionListResponse = SubmissionSummary[]
