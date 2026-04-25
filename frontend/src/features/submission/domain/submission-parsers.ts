import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
import type { SubmissionSort } from '@/features/submission/model/SubmissionSort'
import type { SubmissionSortDirection } from '@/features/submission/model/SubmissionSortDirection'
import type { SubmissionVerdictFilter } from '@/features/submission/model/SubmissionVerdictFilter'
import type { SubmissionSourceCode } from '@/features/submission/model/SubmissionSourceCode'
import type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
export type ParseResult<T> = ParseSuccess<T> | ParseFailure

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
const supportedSubmissionVerdictFilters = [
  'all',
  'pending',
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]
const supportedSubmissionSorts = ['submitted', 'time', 'memory', 'code_length'] as const satisfies readonly SubmissionSort[]
const supportedSubmissionSortDirections = ['asc', 'desc'] as const satisfies readonly SubmissionSortDirection[]

function createSubmissionId(value: number): SubmissionId {
  return value as SubmissionId
}

function createSubmissionSourceCode(value: string): SubmissionSourceCode {
  return value as SubmissionSourceCode
}

export function requireParsed<T>(result: ParseResult<T>, label: string): T {
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

export function isSubmissionVerdictFilter(value: string): value is SubmissionVerdictFilter {
  return supportedSubmissionVerdictFilters.includes(value as SubmissionVerdictFilter)
}

export function isSubmissionSort(value: string): value is SubmissionSort {
  return supportedSubmissionSorts.includes(value as SubmissionSort)
}

export function isSubmissionSortDirection(value: string): value is SubmissionSortDirection {
  return supportedSubmissionSortDirections.includes(value as SubmissionSortDirection)
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
