import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'

/** 查询公开题目搜索建议；输入搜索词，输出可展示题目引用列表。 */
export class ListProblemSuggestions implements APIMessage<ProblemSuggestion[]> {
  declare readonly responseType?: ProblemSuggestion[]
  readonly method = 'GET'
  readonly apiPath: string

  constructor(query: ProblemSearchQuery) {
    const params = new URLSearchParams()
    params.set('q', query)
    this.apiPath = `problem-suggestions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
