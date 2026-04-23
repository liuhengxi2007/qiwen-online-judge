import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
import type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type SubmissionSummary = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  canViewDetail: boolean
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  codeLength: number
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}
