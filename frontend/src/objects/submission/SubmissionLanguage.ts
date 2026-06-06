export type SubmissionLanguage = 'cpp17' | 'python3' | 'text'

const supportedSubmissionLanguages = ['cpp17', 'python3', 'text'] as const satisfies readonly SubmissionLanguage[]

export function isSubmissionLanguage(value: string): value is SubmissionLanguage {
  return supportedSubmissionLanguages.includes(value as SubmissionLanguage)
}
