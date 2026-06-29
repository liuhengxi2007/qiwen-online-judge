import type { APIMessage } from '@/system/api/api-message'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemDataManifest } from '@/objects/problem/ProblemDataManifest'

/** 判题数据清单请求体；同时携带内部 ID 和公开 slug 用于 worker 校验。 */
type GetJudgeProblemDataManifestBody = {
  problemId: ProblemId
  problemSlug: ProblemSlug
}

/** 获取判题侧题目数据清单；输出清单或空值，供 worker 拉取数据前使用。 */
export class GetJudgeProblemDataManifest implements APIMessage<ProblemDataManifest | null> {
  declare readonly responseType?: ProblemDataManifest | null
  readonly method = 'POST'
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
