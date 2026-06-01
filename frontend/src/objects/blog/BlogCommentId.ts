export type BlogCommentId = number & { readonly __brand: 'BlogCommentId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createBlogCommentId(value: number): BlogCommentId {
  return value as BlogCommentId
}

export function blogCommentIdValue(id: BlogCommentId): number {
  return id
}

export function parseBlogCommentId(rawId: number): ParseResult<BlogCommentId> {
  if (!Number.isSafeInteger(rawId) || rawId <= 0) {
    return { ok: false, error: 'Blog comment id must be a positive integer.' }
  }
  return { ok: true, value: createBlogCommentId(rawId) }
}