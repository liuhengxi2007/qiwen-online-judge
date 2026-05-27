import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { parseProblemSearchQuery } from '@/objects/problem/problem-parsers'
import { fromProblemSuggestionContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function listProblemSuggestions(query: string): Promise<ProblemSuggestion[]> {
  const parsedQuery = parseProblemSearchQuery(query)
  if (!parsedQuery.ok) {
    return []
  }

  const url = new URL('/api/problems/suggestions', window.location.origin)
  url.searchParams.set('q', parsedQuery.value)
  return requestJson(url.pathname + url.search, (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid problem suggestion payload.')
    }

    return value.map(fromProblemSuggestionContract)
  })
}
