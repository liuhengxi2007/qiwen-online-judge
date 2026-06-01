import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'

type GetSubmissionJudgeStateBody = {
  submissionId: SubmissionId
}

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
