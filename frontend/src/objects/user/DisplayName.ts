export type DisplayName = string & { readonly __brand: 'DisplayName' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createDisplayName(value: string): DisplayName {
  return value as DisplayName
}

export function displayNameValue(displayName: DisplayName): string {
  return displayName
}

export function parseDisplayName(rawDisplayName: string): ParseResult<DisplayName> {
  const normalized = rawDisplayName.trim()

  if (!normalized) {
    return { ok: false, error: 'Display name is required.' }
  }

  if (normalized.length > 120) {
    return { ok: false, error: 'Display name must be at most 120 characters.' }
  }

  return { ok: true, value: createDisplayName(normalized) }
}