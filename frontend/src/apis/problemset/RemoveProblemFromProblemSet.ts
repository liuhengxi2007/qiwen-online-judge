import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 从题集移除题目；输入题集 slug 和题目 slug，输出更新后的题集详情。 */
export class RemoveProblemFromProblemSet implements APIWithSessionMessage<ProblemSetDetail> {
  declare readonly responseType?: ProblemSetDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSetSlug: ProblemSetSlug, problemSlug: ProblemSlug) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}/problems/${problemSlugValue(problemSlug)}/remove`
  }

  body(): undefined {
    return undefined
  }
}
