import type {
  CreateSubmissionRequest as CreateSubmissionRequestContract,
  SubmissionDetail as SubmissionDetailContract,
  SubmissionLanguage as SubmissionLanguageContract,
  SubmissionListResponse as SubmissionListResponseContract,
  SubmissionStatus as SubmissionStatusContract,
  SubmissionVerdict as SubmissionVerdictContract,
  SubmissionSummary as SubmissionSummaryContract,
} from '@contracts/submission'
import type { Username } from '@/features/auth/domain/auth'
import { parseUsername } from '@/features/auth/domain/auth'
import type { ProblemId, ProblemSlug } from '@/features/problem/domain/problem'
import { parseProblemId, parseProblemSlug, problemSlugValue } from '@/features/problem/domain/problem'

type Brand<T, Name extends string> = T & { readonly __brand: Name }
type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

export type SubmissionId = Brand<number, 'SubmissionId'>
export type SubmissionSourceCode = Brand<string, 'SubmissionSourceCode'>
export type SubmissionLanguage = SubmissionLanguageContract
export type SubmissionStatus = SubmissionStatusContract
export type SubmissionVerdict = SubmissionVerdictContract

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

export type CreateSubmissionRequest = {
  problemSlug: ProblemSlug
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}

export type SubmissionListResponse = SubmissionSummary[]

const supportedSubmissionLanguages = ['cpp17', 'python3'] as const satisfies readonly SubmissionLanguage[]
const supportedSubmissionStatuses = ['queued', 'running', 'completed', 'failed'] as const satisfies readonly SubmissionStatus[]
const supportedSubmissionVerdicts = [
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdict[]

function createSubmissionId(value: number): SubmissionId {
  return value as SubmissionId
}

function createSubmissionSourceCode(value: string): SubmissionSourceCode {
  return value as SubmissionSourceCode
}

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function submissionIdValue(submissionId: SubmissionId): number {
  return submissionId
}

export function submissionSourceCodeValue(sourceCode: SubmissionSourceCode): string {
  return sourceCode
}

export function isSubmissionLanguage(value: string): value is SubmissionLanguage {
  return supportedSubmissionLanguages.includes(value as SubmissionLanguage)
}

export function submissionLanguageLabel(language: SubmissionLanguage): string {
  switch (language) {
    case 'cpp17':
      return 'C++17'
    case 'python3':
      return 'Python 3'
  }
}

export function isSubmissionStatus(value: string): value is SubmissionStatus {
  return supportedSubmissionStatuses.includes(value as SubmissionStatus)
}

export function submissionStatusLabel(status: SubmissionStatus): string {
  switch (status) {
    case 'queued':
      return 'Queued'
    case 'running':
      return 'Running'
    case 'completed':
      return 'Completed'
    case 'failed':
      return 'Failed'
  }
}

export function isTerminalSubmissionStatus(status: SubmissionStatus): boolean {
  switch (status) {
    case 'completed':
    case 'failed':
      return true
    case 'queued':
    case 'running':
      return false
  }
}

export function isSubmissionVerdict(value: string): value is SubmissionVerdict {
  return supportedSubmissionVerdicts.includes(value as SubmissionVerdict)
}

export function submissionVerdictLabel(verdict: SubmissionVerdict | null): string {
  switch (verdict) {
    case null:
      return 'Pending'
    case 'accepted':
      return 'Accepted'
    case 'wrong_answer':
      return 'Wrong Answer'
    case 'compile_error':
      return 'Compile Error'
    case 'runtime_error':
      return 'Runtime Error'
    case 'time_limit_exceeded':
      return 'Time Limit Exceeded'
    case 'system_error':
      return 'System Error'
  }
}

export function parseSubmissionId(rawId: number): ParseResult<SubmissionId> {
  if (!Number.isInteger(rawId)) {
    return { ok: false, error: 'Submission id must be an integer.' }
  }

  if (rawId < 1) {
    return { ok: false, error: 'Submission id is required.' }
  }

  return { ok: true, value: createSubmissionId(rawId) }
}

export function parseSubmissionSourceCode(rawSourceCode: string): ParseResult<SubmissionSourceCode> {
  if (!rawSourceCode.trim()) {
    return { ok: false, error: 'Source code is required.' }
  }

  if (rawSourceCode.length > 200_000) {
    return { ok: false, error: 'Source code must be at most 200000 characters.' }
  }

  return { ok: true, value: createSubmissionSourceCode(rawSourceCode) }
}

export function fromSubmissionDetailContract(submission: SubmissionDetailContract): SubmissionDetail {
  return {
    id: requireParsed(parseSubmissionId(submission.id), 'submission id'),
    problemId: requireParsed(parseProblemId(submission.problemId), 'submission problem id'),
    problemSlug: requireParsed(parseProblemSlug(submission.problemSlug), 'submission problem slug'),
    submitterUsername: requireParsed(parseUsername(submission.submitterUsername), 'submission submitter username'),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    judgeMessage: submission.judgeMessage,
    sourceCode: requireParsed(parseSubmissionSourceCode(submission.sourceCode), 'submission source code'),
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionSummaryContract(submission: SubmissionSummaryContract): SubmissionSummary {
  return {
    id: requireParsed(parseSubmissionId(submission.id), 'submission id'),
    problemId: requireParsed(parseProblemId(submission.problemId), 'submission problem id'),
    problemSlug: requireParsed(parseProblemSlug(submission.problemSlug), 'submission problem slug'),
    submitterUsername: requireParsed(parseUsername(submission.submitterUsername), 'submission submitter username'),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionListResponseContract(response: SubmissionListResponseContract): SubmissionListResponse {
  return response.map(fromSubmissionSummaryContract)
}

export function toCreateSubmissionRequestContract(request: CreateSubmissionRequest): CreateSubmissionRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
    language: request.language,
    sourceCode: submissionSourceCodeValue(request.sourceCode),
  }
}
