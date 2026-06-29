import type { APIMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'

/** 内部更新提交判题状态请求体；由 worker 回传状态快照。 */
type UpdateSubmissionJudgeStateBody = {
  submissionId: SubmissionId
  judgeState: SubmissionJudgeState
}

/** 更新提交判题状态的内部 API；输入提交 ID 和状态，输出通用成功响应。 */
export class UpdateSubmissionJudgeState implements APIMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/state/update'
  private readonly submissionId: SubmissionId
  private readonly judgeState: SubmissionJudgeState

  constructor(submissionId: SubmissionId, judgeState: SubmissionJudgeState) {
    this.submissionId = submissionId
    this.judgeState = judgeState
  }

  body(): UpdateSubmissionJudgeStateBody {
    return { submissionId: this.submissionId, judgeState: this.judgeState }
  }
}
