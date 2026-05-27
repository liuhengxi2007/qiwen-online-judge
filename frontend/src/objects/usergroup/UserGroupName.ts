export type UserGroupName = string & { readonly __brand: 'UserGroupName' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createUserGroupName(value: string): UserGroupName {
  return value as UserGroupName
}

export function userGroupNameValue(name: UserGroupName): string {
  return name
}

export function parseUserGroupName(rawName: string): ParseResult<UserGroupName> {
  const normalized = rawName.trim()
  if (!normalized) {
    return { ok: false, error: 'User group name is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'User group name must be at most 120 characters.' }
  }

  return { ok: true, value: createUserGroupName(normalized) }
}

export function fromUserGroupNameContract(value: string, label: string): UserGroupName {
  const result = parseUserGroupName(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserGroupNameContract(value: UserGroupName): string {
  return userGroupNameValue(value)
}
