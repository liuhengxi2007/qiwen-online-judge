import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataTreeResponse } from '@/objects/problem/response/ProblemDataTreeResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 列出题目数据树；输入题目 slug 和可选比赛上下文，输出文件/目录节点。 */
export class ListProblemDataTree implements APIWithSessionMessage<ProblemDataTreeResponse> {
  declare readonly responseType?: ProblemDataTreeResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/tree${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
