import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

/** 内部领取 Hack 判题请求体；描述 worker 支持语言和开始时间。 */
type ClaimNextHackAttemptBody = {
  languages: SubmissionLanguage[]
  startedAt: string
}

/** Hack worker 领取下一个 Hack 尝试；输出未建模的任务载荷或空值。 */
/** FIXME-CN: 后端返回 Option[ClaimedHackAttempt]，前端用 unknown 无法校验 worker 任务载荷，应镜像内部响应类型。 */
export class ClaimNextHackAttempt implements APIMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/claim-next'
  private readonly languages: SubmissionLanguage[]
  private readonly startedAt: string

  constructor(languages: SubmissionLanguage[], startedAt: string) {
    this.languages = languages
    this.startedAt = startedAt
  }

  body(): ClaimNextHackAttemptBody {
    return { languages: this.languages, startedAt: this.startedAt }
  }
}
