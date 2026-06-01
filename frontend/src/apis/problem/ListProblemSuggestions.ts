import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { readArray } from '@/objects/shared/PageResponse'
import { fromProblemSuggestionContract } from '@/objects/problem/response/ProblemSuggestion'

export class ListProblemSuggestions implements APIMessage<ProblemSuggestion[]> {
  declare readonly responseType?: ProblemSuggestion[]
  readonly method = 'GET'
  readonly decode = (value: unknown) => readArray(value, 'problem suggestions', fromProblemSuggestionContract)
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
