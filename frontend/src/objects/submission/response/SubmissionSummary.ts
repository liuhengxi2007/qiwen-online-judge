import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import type { SubmissionSource } from '@/objects/submission/SubmissionSource'

export type SubmissionSummary = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  source: SubmissionSource
  canViewDetail: boolean
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  score: number | null
  codeLength: number
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}
