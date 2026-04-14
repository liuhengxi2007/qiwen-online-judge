import type { Username } from '@/features/auth/model/AuthValues'
import type { ProblemId, ProblemSlug } from '@/features/problem/model/Problem'

export type SubmissionId = number & { readonly __brand: 'SubmissionId' }
export type SubmissionSourceCode = string & { readonly __brand: 'SubmissionSourceCode' }
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
  problemSlug: ProblemSlug
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}

export type SubmissionSummary = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  submitterUsername: Username
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionDetail = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  submitterUsername: Username
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
  sourceCode: SubmissionSourceCode
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionListResponse = SubmissionSummary[]
