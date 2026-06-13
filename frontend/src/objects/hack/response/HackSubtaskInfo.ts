import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 可 Hack 子任务信息；用于创建 Hack 前展示目标、模式和策略要求。 */
export type HackSubtaskInfo = {
  targetSubmissionId: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  targetSubmitter: UserIdentity
  subtaskIndex: number
  subtaskLabel: string | null
  oldWorstScore: number
  /** FIXME-CN: mode 是判题配置中的 hack answer generation mode，当前裸 string 容易和未知模式混用，应镜像后端/协议枚举。 */
  mode: string
  requiresStrategyProvider: boolean
}
