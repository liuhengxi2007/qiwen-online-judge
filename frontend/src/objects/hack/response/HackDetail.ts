import type { HackId } from '@/objects/hack/HackId'
import type { HackStatus } from '@/objects/hack/HackStatus'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { UserIdentity } from '@/objects/user/UserIdentity'

export type HackDetail = {
  id: HackId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  targetSubmissionId: SubmissionId
  targetSubmitter: UserIdentity
  author: UserIdentity
  subtaskIndex: number
  subtaskLabel: string | null
  status: HackStatus
  input: string
  strategyProviderSource: string | null
  answer: string | null
  oldScore: number
  newScore: number | null
  validatorMessage: string | null
  standardMessage: string | null
  targetMessage: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
}
