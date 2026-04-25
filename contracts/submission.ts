import type { UserIdentity } from './auth'
import type { PageResponse } from './shared'

export type SubmissionLanguage = 'cpp17' | 'python3'
export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'
export type SubmissionVerdict =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'

export type SubmissionVerdictFilter =
  | 'all'
  | 'pending'
  | SubmissionVerdict

export type SubmissionSort = 'submitted' | 'time' | 'memory' | 'code_length'

export type SubmissionSortDirection = 'asc' | 'desc'

export type CreateSubmissionRequest = {
  problemSlug: string
  language: SubmissionLanguage
  sourceCode: string
}

export type SubmissionListRequest = {
  userQuery: string | null
  problemQuery: string | null
  verdict: SubmissionVerdictFilter
  sort: SubmissionSort
  direction: SubmissionSortDirection
  page: number
  pageSize: number
}

export type SubmissionSummary = {
  id: number
  problemId: string
  problemSlug: string
  problemTitle: string
  canViewDetail: boolean
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  codeLength: number
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionDetail = {
  id: number
  problemId: string
  problemSlug: string
  problemTitle: string
  canManage: boolean
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  judgeMessage: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  codeLength: number
  sourceCode: string
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export type SubmissionListResponse = PageResponse<SubmissionSummary>
