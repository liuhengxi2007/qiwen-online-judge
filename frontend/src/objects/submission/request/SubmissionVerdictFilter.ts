import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

export type SubmissionVerdictFilter = 'all' | 'pending' | SubmissionVerdict

const supportedSubmissionVerdictFilters = [
  'all',
  'pending',
  'accepted',
  'accepted_by_protocol',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

export function isSubmissionVerdictFilter(value: string): value is SubmissionVerdictFilter {
  return supportedSubmissionVerdictFilters.includes(value as SubmissionVerdictFilter)
}
