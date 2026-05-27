export type UserGroupDescription = string & { readonly __brand: 'UserGroupDescription' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createUserGroupDescription(value: string): UserGroupDescription {
  return value as UserGroupDescription
}

export function userGroupDescriptionValue(description: UserGroupDescription): string {
  return description
}

export function parseUserGroupDescription(rawDescription: string): ParseResult<UserGroupDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 2000) {
    return { ok: false, error: 'User group description must be at most 2000 characters.' }
  }

  return { ok: true, value: createUserGroupDescription(normalized) }
}

export function fromUserGroupDescriptionContract(
  value: string,
  label: string,
): UserGroupDescription {
  const result = parseUserGroupDescription(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserGroupDescriptionContract(value: UserGroupDescription): string {
  return userGroupDescriptionValue(value)
}
