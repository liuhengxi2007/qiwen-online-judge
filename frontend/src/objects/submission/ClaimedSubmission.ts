import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

export type ClaimedSubmission = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}
