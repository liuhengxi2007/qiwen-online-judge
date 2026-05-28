import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'

export class ListProblemSuggestions implements APIMessage<ProblemSuggestion[]> {
  declare readonly responseType?: ProblemSuggestion[]
  readonly method = 'GET'
  readonly apiPath: string

  constructor(query: ProblemSearchQuery) {
    const params = new URLSearchParams()
    params.set('q', query)
    this.apiPath = `problems/suggestions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
