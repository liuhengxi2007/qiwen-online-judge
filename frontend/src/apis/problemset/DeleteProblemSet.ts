import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 删除题集；输入题集 slug，输出通用成功响应，权限由后端校验。 */
export class DeleteProblemSet implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSetSlug: ProblemSetSlug) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
