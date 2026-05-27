export type UserGroupSlug = string & { readonly __brand: 'UserGroupSlug' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createUserGroupSlug(value: string): UserGroupSlug {
  return value as UserGroupSlug
}

export function userGroupSlugValue(slug: UserGroupSlug): string {
  return slug
}

export function parseUserGroupSlug(rawSlug: string): ParseResult<UserGroupSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  return { ok: true, value: createUserGroupSlug(normalized) }
}

export function fromUserGroupSlugContract(value: string, label: string): UserGroupSlug {
  const result = parseUserGroupSlug(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserGroupSlugContract(value: UserGroupSlug): string {
  return userGroupSlugValue(value)
}
