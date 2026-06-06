import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

type ReadHackProblemDataBody = {
  problemSlug: ProblemSlug
  path: string
}

export class ReadHackProblemData implements APIMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/problem-data'
  private readonly problemSlug: ProblemSlug
  private readonly path: string

  constructor(problemSlug: ProblemSlug, path: string) {
    this.problemSlug = problemSlug
    this.path = path
  }

  body(): ReadHackProblemDataBody {
    return { problemSlug: this.problemSlug, path: this.path }
  }
}
