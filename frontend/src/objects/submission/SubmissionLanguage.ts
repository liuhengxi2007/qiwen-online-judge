export type SubmissionLanguage = 'cpp17' | 'python3'

const supportedSubmissionLanguages = ['cpp17', 'python3'] as const satisfies readonly SubmissionLanguage[]

export function isSubmissionLanguage(value: string): value is SubmissionLanguage {
  return supportedSubmissionLanguages.includes(value as SubmissionLanguage)
}