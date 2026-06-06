import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { UserIdentity } from '@/objects/user/UserIdentity'

export type HackSubtaskInfo = {
  targetSubmissionId: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  targetSubmitter: UserIdentity
  subtaskIndex: number
  subtaskLabel: string | null
  oldLowestScore: number
  mode: string
  requiresStrategyProvider: boolean
}
