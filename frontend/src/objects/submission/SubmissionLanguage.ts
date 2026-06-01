export type SubmissionLanguage = 'cpp17' | 'python3'

const supportedSubmissionLanguages = ['cpp17', 'python3'] as const satisfies readonly SubmissionLanguage[]

export function isSubmissionLanguage(value: string): value is SubmissionLanguage {
  return supportedSubmissionLanguages.includes(value as SubmissionLanguage)
}

export function fromSubmissionLanguageContract(value: unknown): SubmissionLanguage {
  if (typeof value !== 'string' || !isSubmissionLanguage(value)) {
    throw new Error('Invalid submission language in contract payload.')
  }

  return value
}

export function toSubmissionLanguageContract(value: SubmissionLanguage): SubmissionLanguage {
  return value
}
