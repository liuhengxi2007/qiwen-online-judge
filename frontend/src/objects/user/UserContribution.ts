export type UserContribution = number & { readonly __brand: 'UserContribution' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createUserContribution(value: number): UserContribution {
  return value as UserContribution
}

export function userContributionValue(contribution: UserContribution): number {
  return contribution
}

export function parseUserContribution(rawContribution: number): ParseResult<UserContribution> {
  if (!Number.isFinite(rawContribution)) {
    return { ok: false, error: 'User contribution must be a finite number.' }
  }

  return { ok: true, value: createUserContribution(rawContribution) }
}