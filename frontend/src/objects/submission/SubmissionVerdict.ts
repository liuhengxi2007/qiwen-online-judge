export type SubmissionVerdict =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'

const supportedSubmissionVerdicts = [
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdict[]

export function isSubmissionVerdict(value: string): value is SubmissionVerdict {
  return supportedSubmissionVerdicts.includes(value as SubmissionVerdict)
}