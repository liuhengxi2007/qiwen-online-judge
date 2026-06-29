import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'

/** 提交判题状态快照；内部 worker 接口用它同步状态和最终结果。 */
export type SubmissionJudgeState = {
  status: SubmissionStatus
  judgeResult: JudgeResult | null
  startedAt: string | null
  finishedAt: string | null
}
