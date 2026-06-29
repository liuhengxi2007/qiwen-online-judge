import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 查询当前会话可管理题目建议；可选比赛上下文会影响可选题目范围。 */
export class ListManageableProblemSuggestions implements APIWithSessionMessage<ProblemSuggestion[]> {
  declare readonly responseType?: ProblemSuggestion[]
  readonly method = 'GET'
  readonly apiPath: string

  constructor(query: ProblemSearchQuery, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    params.set('q', query)
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problem-suggestions/manageable?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
