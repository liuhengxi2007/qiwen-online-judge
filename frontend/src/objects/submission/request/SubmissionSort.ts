export type SubmissionSort = 'submitted' | 'time' | 'memory' | 'code_length'

const supportedSubmissionSorts = ['submitted', 'time', 'memory', 'code_length'] as const satisfies readonly SubmissionSort[]

export function isSubmissionSort(value: string): value is SubmissionSort {
  return supportedSubmissionSorts.includes(value as SubmissionSort)
}
