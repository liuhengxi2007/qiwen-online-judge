export type SubmissionResultDisplayMode = 'verdict' | 'score'

const supportedSubmissionResultDisplayModes = [
  'verdict',
  'score',
] as const satisfies readonly SubmissionResultDisplayMode[]

export function isSubmissionResultDisplayMode(value: string): value is SubmissionResultDisplayMode {
  return supportedSubmissionResultDisplayModes.includes(value as SubmissionResultDisplayMode)
}
