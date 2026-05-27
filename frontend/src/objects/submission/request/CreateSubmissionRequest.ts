import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

export type CreateSubmissionRequest = {
  problemSlug: ProblemSlug
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}
