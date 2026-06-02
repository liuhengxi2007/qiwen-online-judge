export type ContestSlug = string & { readonly __brand: 'ContestSlug' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createContestSlug(value: string): ContestSlug {
  return value as ContestSlug
}

export function contestSlugValue(slug: ContestSlug): string {
  return slug
}

export function parseContestSlug(rawSlug: string): ParseResult<ContestSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Contest slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Contest slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Contest slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createContestSlug(normalized) }
}
