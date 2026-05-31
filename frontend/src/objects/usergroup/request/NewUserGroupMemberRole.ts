export type NewUserGroupMemberRole = 'manager' | 'member'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function parseNewUserGroupMemberRole(rawRole: string): ParseResult<NewUserGroupMemberRole> {
  if (rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'New members may only be added as member or manager.' }
}

export function fromNewUserGroupMemberRoleContract(
  value: NewUserGroupMemberRole,
  label: string,
): NewUserGroupMemberRole {
  const result = parseNewUserGroupMemberRole(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toNewUserGroupMemberRoleContract(
  value: NewUserGroupMemberRole,
): NewUserGroupMemberRole {
  return value
}
