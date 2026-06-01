export type Username = string & { readonly __brand: 'Username' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const usernamePattern = /^[a-z0-9_-]+$/

function createUsername(value: string): Username {
  return value as Username
}

export function usernameValue(username: Username): string {
  return username
}

export function parseUsername(rawUsername: string): ParseResult<Username> {
  const normalized = rawUsername.trim().toLowerCase()

  if (!normalized) {
    return { ok: false, error: 'Username is required.' }
  }

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: createUsername(normalized) }
}