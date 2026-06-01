export type BlogCommentContent = string & { readonly __brand: 'BlogCommentContent' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createBlogCommentContent(value: string): BlogCommentContent {
  return value as BlogCommentContent
}

export function blogCommentContentValue(content: BlogCommentContent): string {
  return content
}

export function parseBlogCommentContent(rawContent: string): ParseResult<BlogCommentContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Comment content is required.' }
  }
  if (normalized.length > 20000) {
    return { ok: false, error: 'Comment content must be at most 20000 characters.' }
  }
  return { ok: true, value: createBlogCommentContent(normalized) }
}