export type AccessUserGroupSlug = string & { readonly __brand: 'AccessUserGroupSlug' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const userGroupSlugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

export function accessUserGroupSlugValue(slug: AccessUserGroupSlug): string {
  return slug
}

export function parseAccessUserGroupSlug(rawSlug: string): ParseResult<AccessUserGroupSlug> {
  const normalized = rawSlug.trim()

  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }

  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }

  if (!userGroupSlugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  return { ok: true, value: normalized as AccessUserGroupSlug }
}
