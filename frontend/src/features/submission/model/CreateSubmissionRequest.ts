import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/features/submission/model/SubmissionSourceCode'

export type CreateSubmissionRequest = {
  problemSlug: ProblemSlug
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}
