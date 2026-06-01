export type PlaintextPassword = string & { readonly __brand: 'PlaintextPassword' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createPlaintextPassword(value: string): PlaintextPassword {
  return value as PlaintextPassword
}

export function plaintextPasswordValue(password: PlaintextPassword): string {
  return password
}

export function parsePlaintextPassword(rawPassword: string): ParseResult<PlaintextPassword> {
  const normalized = rawPassword.trim()

  if (!normalized) {
    return { ok: false, error: 'Password is required.' }
  }

  return { ok: true, value: createPlaintextPassword(normalized) }
}