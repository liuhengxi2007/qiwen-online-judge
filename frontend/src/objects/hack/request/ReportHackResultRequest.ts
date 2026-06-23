import type { HackStatus } from '@/objects/hack/HackStatus'
import type { JudgeResult } from '@/objects/submission/JudgeResult'

/** Judger 完成 hack attempt 后回报的结果载荷；对象对齐例外：镜像 judge-protocol ReportHackResultRequest，不对应后端 hack domain object。 */
export type ReportHackResultRequest = {
  status: HackStatus
  answer: string | null
  newResult: JudgeResult | null
  validatorMessage: string | null
  standardMessage: string | null
  targetMessage: string | null
}
