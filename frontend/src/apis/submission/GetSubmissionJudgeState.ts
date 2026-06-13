import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'

/** 内部获取提交判题状态请求体；提交 ID 通过 body 传递。 */
type GetSubmissionJudgeStateBody = {
  submissionId: SubmissionId
}

/** 获取提交判题状态的内部 API；输出状态快照或空值，供 worker/轮询使用。 */
export class GetSubmissionJudgeState implements APIMessage<SubmissionJudgeState | null> {
  declare readonly responseType?: SubmissionJudgeState | null
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/state'
  private readonly submissionId: SubmissionId

  constructor(submissionId: SubmissionId) {
    this.submissionId = submissionId
  }

  body(): GetSubmissionJudgeStateBody {
    return { submissionId: this.submissionId }
  }
}
