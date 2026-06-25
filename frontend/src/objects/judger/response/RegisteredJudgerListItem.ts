import type { JudgerId } from '@/objects/judger/JudgerId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

/** 已注册 judger 列表项；用于管理视图查看 worker 能力和心跳状态。 */
export type RegisteredJudgerListItem = {
  judgerId: JudgerId
  requestedPrefix: JudgerId
  host: string
  processId: string | null
  supportedLanguages: SubmissionLanguage[]
  registeredAt: string
  lastHeartbeatAt: string
}
