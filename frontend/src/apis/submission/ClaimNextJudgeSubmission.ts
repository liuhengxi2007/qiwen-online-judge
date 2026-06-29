import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { ClaimedSubmission } from '@/objects/submission/ClaimedSubmission'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'

/** 内部领取提交判题任务请求体；描述 worker 支持语言、运行状态和最低优先级。 */
type ClaimNextJudgeSubmissionBody = {
  languages: SubmissionLanguage[]
  runningState: SubmissionJudgeState
  minPriority: number
}

/** 判题 worker 领取下一个提交任务；输出提交任务或空值，不使用用户会话。 */
export class ClaimNextJudgeSubmission implements APIMessage<ClaimedSubmission | null> {
  declare readonly responseType?: ClaimedSubmission | null
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/claim-next'
  private readonly languages: SubmissionLanguage[]
  private readonly runningState: SubmissionJudgeState
  private readonly minPriority: number

  constructor(languages: SubmissionLanguage[], runningState: SubmissionJudgeState, minPriority: number) {
    this.languages = languages
    this.runningState = runningState
    this.minPriority = minPriority
  }

  body(): ClaimNextJudgeSubmissionBody {
    return { languages: this.languages, runningState: this.runningState, minPriority: this.minPriority }
  }
}
