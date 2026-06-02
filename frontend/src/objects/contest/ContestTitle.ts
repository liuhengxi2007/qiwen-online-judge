export type ContestTitle = string & { readonly __brand: 'ContestTitle' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createContestTitle(value: string): ContestTitle {
  return value as ContestTitle
}

export function contestTitleValue(title: ContestTitle): string {
  return title
}

export function parseContestTitle(rawTitle: string): ParseResult<ContestTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Contest title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Contest title must be at most 120 characters.' }
  }
  return { ok: true, value: createContestTitle(normalized) }
}
