import type { Username } from '@/features/auth/model/AuthValues'
import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
import type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'

export type SubmissionSummary = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  submitterUsername: Username
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}
