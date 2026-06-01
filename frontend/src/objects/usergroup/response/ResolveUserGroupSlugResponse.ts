export type ResolveUserGroupSlugResponse = {
  exists: boolean
}

export function fromResolveUserGroupSlugResponseContract(
  value: unknown,
  label = 'resolve user group slug response',
): ResolveUserGroupSlugResponse {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const response = value as Record<string, unknown>
  if (typeof response.exists !== 'boolean') {
    throw new Error(`Invalid ${label} exists in contract payload: expected boolean.`)
  }

  return { exists: response.exists }
}
