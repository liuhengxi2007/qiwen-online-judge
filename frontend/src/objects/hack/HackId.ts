export type HackId = number & { readonly __brand: 'HackId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createHackId(value: number): HackId {
  return value as HackId
}

export function hackIdValue(hackId: HackId): number {
  return hackId
}

export function parseHackId(rawId: number): ParseResult<HackId> {
  if (!Number.isSafeInteger(rawId)) {
    return { ok: false, error: 'Hack id must be an integer.' }
  }

  if (rawId < 1) {
    return { ok: false, error: 'Hack id is required.' }
  }

  return { ok: true, value: createHackId(rawId) }
}
