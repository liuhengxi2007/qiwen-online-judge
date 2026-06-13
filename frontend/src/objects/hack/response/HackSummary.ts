import type { HackId } from '@/objects/hack/HackId'
import type { HackStatus } from '@/objects/hack/HackStatus'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** Hack 摘要响应；用于列表展示目标、作者、状态和分数变化。 */
export type HackSummary = {
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
  oldScore: number
  newScore: number | null
  createdAt: string
  finishedAt: string | null
}
