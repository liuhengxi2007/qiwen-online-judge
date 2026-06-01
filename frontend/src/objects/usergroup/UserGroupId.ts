export type UserGroupId = string & { readonly __brand: 'UserGroupId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createUserGroupId(value: string): UserGroupId {
  return value as UserGroupId
}

export function parseUserGroupId(rawId: string): ParseResult<UserGroupId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'User group id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'User group id must be a valid UUID.' }
  }

  return { ok: true, value: createUserGroupId(normalized) }
}