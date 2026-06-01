import type { APIMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'
import { decodeSuccessResponse } from '@/system/api/http-client'

type UpdateSubmissionJudgeStateBody = {
  submissionId: SubmissionId
  judgeState: SubmissionJudgeState
}

export class UpdateSubmissionJudgeState implements APIMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly decode = decodeSuccessResponse
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
