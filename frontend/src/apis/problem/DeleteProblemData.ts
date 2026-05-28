import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class DeleteProblemData implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, filename: ProblemDataFilename) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
