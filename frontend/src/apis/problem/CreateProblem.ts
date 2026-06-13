import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateProblemRequest } from '@/objects/problem/request/CreateProblemRequest'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

/** 创建题目；输入题目基础资料和访问策略，输出新题目详情。 */
export class CreateProblem implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath = 'problems'
  private readonly request: CreateProblemRequest

  constructor(request: CreateProblemRequest) {
    this.request = request
  }

  body(): CreateProblemRequest {
    return this.request
  }
}
