import type { HackStatus } from '@/objects/hack/HackStatus'
import type { JudgeResult } from '@/objects/submission/JudgeResult'

/** Judger 完成 hack attempt 后回报的结果载荷；镜像 judge-protocol ReportHackResultRequest。 */
export type ReportHackResultRequest = {
  status: HackStatus
  answer: string | null
  oldScore: number
  newScore: number | null
  newResult: JudgeResult | null
  validatorMessage: string | null
  standardMessage: string | null
  targetMessage: string | null
}
