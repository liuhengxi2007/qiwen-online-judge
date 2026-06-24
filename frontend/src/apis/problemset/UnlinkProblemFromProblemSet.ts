import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 从题单解除题目关联；输入题单 slug 和题目 slug，输出更新后的题单详情。 */
export class UnlinkProblemFromProblemSet implements APIWithSessionMessage<ProblemSetDetail> {
  declare readonly responseType?: ProblemSetDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSetSlug: ProblemSetSlug, problemSlug: ProblemSlug) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}/problems/${problemSlugValue(problemSlug)}/unlink`
  }

  body(): undefined {
    return undefined
  }
}
