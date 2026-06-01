import type { APIMessage } from '@/system/api/api-message'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemDataManifest } from '@/objects/problem/ProblemDataManifest'
import { readNullable } from '@/objects/shared/PageResponse'
import { fromProblemDataManifestContract } from '@/objects/problem/ProblemDataManifest'

type GetJudgeProblemDataManifestBody = {
  problemId: ProblemId
  problemSlug: ProblemSlug
}

export class GetJudgeProblemDataManifest implements APIMessage<ProblemDataManifest | null> {
  declare readonly responseType?: ProblemDataManifest | null
  readonly method = 'POST'
  readonly decode = (value: unknown) => readNullable(value, 'problem data manifest', fromProblemDataManifestContract)
  readonly apiPath = 'internal/problems/judge-data-manifest'
  private readonly problemId: ProblemId
  private readonly problemSlug: ProblemSlug

  constructor(problemId: ProblemId, problemSlug: ProblemSlug) {
    this.problemId = problemId
    this.problemSlug = problemSlug
  }

  body(): GetJudgeProblemDataManifestBody {
    return { problemId: this.problemId, problemSlug: this.problemSlug }
  }
}
