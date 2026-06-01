export type UserGroupRole = 'owner' | 'manager' | 'member'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function parseUserGroupRole(rawRole: string): ParseResult<UserGroupRole> {
  if (rawRole === 'owner' || rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'Unknown user group role.' }
}