export type BlogContributionResponse = {
  contribution: number
}

export function fromBlogContributionResponseContract(
  value: unknown,
  label = 'blog contribution response',
): BlogContributionResponse {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const response = value as Record<string, unknown>
  if (typeof response.contribution !== 'number' || !Number.isSafeInteger(response.contribution)) {
    throw new Error(`Invalid ${label} contribution in contract payload: expected safe integer.`)
  }

  return { contribution: response.contribution }
}
