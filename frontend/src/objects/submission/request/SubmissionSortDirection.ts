export type SubmissionSortDirection = 'asc' | 'desc'

const supportedSubmissionSortDirections = ['asc', 'desc'] as const satisfies readonly SubmissionSortDirection[]

export function isSubmissionSortDirection(value: string): value is SubmissionSortDirection {
  return supportedSubmissionSortDirections.includes(value as SubmissionSortDirection)
}
