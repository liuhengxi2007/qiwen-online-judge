export type ResolveProblemSetSlugResponse = {
  exists: boolean
}

export function fromResolveProblemSetSlugResponseContract(
  value: unknown,
  label = 'resolve problem set slug response',
): ResolveProblemSetSlugResponse {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const response = value as Record<string, unknown>
  if (typeof response.exists !== 'boolean') {
    throw new Error(`Invalid ${label} exists in contract payload: expected boolean.`)
  }

  return { exists: response.exists }
}
