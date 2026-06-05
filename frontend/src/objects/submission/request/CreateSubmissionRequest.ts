import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

type CreateSubmissionProgram = {
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}

export type CreateSubmissionRequest = {
  problemSlug: ProblemSlug
  programs: Record<string, CreateSubmissionProgram>
}
