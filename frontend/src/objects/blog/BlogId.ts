export type BlogId = number & { readonly __brand: 'BlogId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createBlogId(value: number): BlogId {
  return value as BlogId
}

export function blogIdValue(id: BlogId): number {
  return id
}

export function parseBlogId(rawId: number): ParseResult<BlogId> {
  if (!Number.isSafeInteger(rawId) || rawId <= 0) {
    return { ok: false, error: 'Blog id must be a positive integer.' }
  }
  return { ok: true, value: createBlogId(rawId) }
}

export function fromBlogIdContract(value: number, label: string): BlogId {
  const result = parseBlogId(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}
