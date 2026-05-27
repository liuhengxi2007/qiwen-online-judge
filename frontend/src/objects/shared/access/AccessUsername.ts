export type AccessUsername = string & { readonly __brand: 'AccessUsername' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const usernamePattern = /^[a-z0-9_-]+$/

export function accessUsernameValue(username: AccessUsername): string {
  return username
}

export function parseAccessUsername(rawUsername: string): ParseResult<AccessUsername> {
  const normalized = rawUsername.trim().toLowerCase()

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: normalized as AccessUsername }
}
