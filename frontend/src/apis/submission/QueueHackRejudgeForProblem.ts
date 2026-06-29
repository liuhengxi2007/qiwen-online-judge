import type { APIMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'
import type { ProblemId } from '@/objects/problem/ProblemId'

/** 内部按题目排队 Hack 重测；输入题目 ID 作为 body，输出通用成功响应。 */
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
