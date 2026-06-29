import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'

/** 创建题集；输入基础资料和访问策略，输出新题集摘要。 */
export class CreateProblemSet implements APIWithSessionMessage<ProblemSetSummary> {
  declare readonly responseType?: ProblemSetSummary
  readonly method = 'POST'
  readonly apiPath = 'problem-sets'
  private readonly request: CreateProblemSetRequest

  constructor(request: CreateProblemSetRequest) {
    this.request = request
  }

  body(): CreateProblemSetRequest {
    return this.request
  }
}
