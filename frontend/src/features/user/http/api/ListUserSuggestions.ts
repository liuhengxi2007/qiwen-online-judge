import type { UserIdentity } from '@/features/user/domain/user'
import {
  fromUserIdentityContract,
  parseUserSearchQuery,
} from '@/features/user/domain/user'
import { requestJson } from '@/shared/api/http-client'

export async function listUserSuggestions(query: string): Promise<UserIdentity[]> {
  const parsedQuery = parseUserSearchQuery(query)
  if (!parsedQuery.ok) {
    return []
  }

  const url = new URL('/api/users/suggestions', window.location.origin)
  url.searchParams.set('q', parsedQuery.value)
  return requestJson(url.pathname + url.search, (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid user suggestion payload.')
    }

    return value.map(fromUserIdentityContract)
  })
}
