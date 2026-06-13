import type { SubmissionId } from '@/objects/submission/SubmissionId'

/** 创建 Hack 请求体；input 为攻击数据文本，策略提供器源码可选。 */
export type CreateHackRequest = {
  targetSubmissionId: SubmissionId
  subtaskIndex: number
  input: string
  strategyProviderSource: string | null
}
