import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { DeleteProblemDataPathRequest } from '@/objects/problem/request/DeleteProblemDataPathRequest'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class DeleteProblemDataPath implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: DeleteProblemDataPathRequest

  constructor(problemSlug: ProblemSlug, path: ProblemDataPath) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/file/delete`
    this.request = { path }
  }

  body(): DeleteProblemDataPathRequest {
    return this.request
  }
}
