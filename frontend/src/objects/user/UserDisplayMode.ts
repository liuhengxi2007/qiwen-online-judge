export type UserDisplayMode = 'display_name' | 'username' | 'display_name_with_username'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function userDisplayModeValue(displayMode: UserDisplayMode): UserDisplayMode {
  return displayMode
}

export function parseUserDisplayMode(rawDisplayMode: string): ParseResult<UserDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'display_name':
    case 'username':
    case 'display_name_with_username':
      return { ok: true, value: normalized }
    default:
      return {
        ok: false,
        error: 'Display mode must be one of: display_name, username, display_name_with_username.',
      }
  }
}

export function fromUserDisplayModeContract(value: string, label: string): UserDisplayMode {
  const result = parseUserDisplayMode(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserDisplayModeContract(value: UserDisplayMode): UserDisplayMode {
  return userDisplayModeValue(value)
}
