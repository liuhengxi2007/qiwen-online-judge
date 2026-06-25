import type { APIMessage } from '@/system/api/api-message'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 内部按题目排队手动整题重判；输入题目 ID 作为 body，输出排队数量。 */
export class QueueManualProblemRejudgeForProblem implements APIMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/queue-manual-problem-rejudge'
  private readonly problemId: ProblemId

  constructor(problemId: ProblemId) {
    this.problemId = problemId
  }

  body(): ProblemId {
    return this.problemId
  }
}
