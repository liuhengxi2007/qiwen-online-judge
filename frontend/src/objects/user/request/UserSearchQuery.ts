export type UserSearchQuery = string & { readonly __brand: 'UserSearchQuery' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createUserSearchQuery(value: string): UserSearchQuery {
  return value as UserSearchQuery
}

export function userSearchQueryValue(query: UserSearchQuery): string {
  return query
}

export function parseUserSearchQuery(rawQuery: string): ParseResult<UserSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'User search query is required.' }
  }
  return { ok: true, value: createUserSearchQuery(normalized) }
}
