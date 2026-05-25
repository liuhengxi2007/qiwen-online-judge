import type { ProblemSuggestion } from '@/features/problem/http/response/ProblemSuggestion'
import { parseProblemSearchQuery } from '@/features/problem/lib/problem-parsers'
import { fromProblemSuggestionContract } from '@/features/problem/http/codec/ProblemHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

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
