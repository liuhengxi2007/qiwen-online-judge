export type AddUserGroupMemberRole = 'manager' | 'member'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function parseAddUserGroupMemberRole(rawRole: string): ParseResult<AddUserGroupMemberRole> {
  if (rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'New members may only be added as member or manager.' }
}

export function fromAddUserGroupMemberRoleContract(
  value: AddUserGroupMemberRole,
  label: string,
): AddUserGroupMemberRole {
  const result = parseAddUserGroupMemberRole(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toAddUserGroupMemberRoleContract(
  value: AddUserGroupMemberRole,
): AddUserGroupMemberRole {
  return value
}
