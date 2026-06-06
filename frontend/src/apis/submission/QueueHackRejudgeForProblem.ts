import type { APIMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ProblemId } from '@/objects/problem/ProblemId'

export class QueueHackRejudgeForProblem implements APIMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/queue-hack-rejudge'
  private readonly problemId: ProblemId

  constructor(problemId: ProblemId) {
    this.problemId = problemId
  }

  body(): ProblemId {
    return this.problemId
  }
}
