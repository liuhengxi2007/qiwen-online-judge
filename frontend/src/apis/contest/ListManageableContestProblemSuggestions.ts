import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class ListManageableContestProblemSuggestions implements APIWithSessionMessage<ProblemSuggestion[]> {
  declare readonly responseType?: ProblemSuggestion[]
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, query: ProblemSearchQuery) {
    const params = new URLSearchParams()
    params.set('q', query)
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/problem-suggestions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
