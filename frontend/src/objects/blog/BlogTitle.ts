export type BlogTitle = string & { readonly __brand: 'BlogTitle' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createBlogTitle(value: string): BlogTitle {
  return value as BlogTitle
}

export function blogTitleValue(title: BlogTitle): string {
  return title
}

export function parseBlogTitle(rawTitle: string): ParseResult<BlogTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Blog title is required.' }
  }
  if (normalized.length > 160) {
    return { ok: false, error: 'Blog title must be at most 160 characters.' }
  }
  return { ok: true, value: createBlogTitle(normalized) }
}