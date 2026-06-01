export type UserGroupRole = 'owner' | 'manager' | 'member'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function parseUserGroupRole(rawRole: string): ParseResult<UserGroupRole> {
  if (rawRole === 'owner' || rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'Unknown user group role.' }
}

export function fromUserGroupRoleContract(value: string, label: string): UserGroupRole {
  const result = parseUserGroupRole(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserGroupRoleContract(value: UserGroupRole): UserGroupRole {
  return value
}
