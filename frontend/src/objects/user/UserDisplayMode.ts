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