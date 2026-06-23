import type { HackId } from '@/objects/hack/HackId'
import type { ClaimedSubmission } from '@/objects/submission/ClaimedSubmission'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { Username } from '@/objects/user/Username'

/** Hack worker 领取到的 hack attempt；镜像后端内部 ClaimedHackAttempt。 */
export type ClaimedHackAttempt = {
  hackId: HackId
  targetSubmission: ClaimedSubmission
  authorUsername: Username
  subtaskIndex: number
  input: string
  strategyProviderSource: string | null
  oldResult: JudgeResult
}
