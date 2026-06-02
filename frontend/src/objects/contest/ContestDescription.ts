export type ContestDescription = string & { readonly __brand: 'ContestDescription' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createContestDescription(value: string): ContestDescription {
  return value as ContestDescription
}

export function contestDescriptionValue(description: ContestDescription): string {
  return description
}

export function parseContestDescription(rawDescription: string): ParseResult<ContestDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 4000) {
    return { ok: false, error: 'Contest description must be at most 4000 characters.' }
  }
  return { ok: true, value: createContestDescription(normalized) }
}
