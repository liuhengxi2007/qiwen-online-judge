export type BlogContent = string & { readonly __brand: 'BlogContent' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createBlogContent(value: string): BlogContent {
  return value as BlogContent
}

export function blogContentValue(content: BlogContent): string {
  return content
}

export function parseBlogContent(rawContent: string): ParseResult<BlogContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Blog content is required.' }
  }
  if (normalized.length > 200000) {
    return { ok: false, error: 'Blog content must be at most 200000 characters.' }
  }
  return { ok: true, value: createBlogContent(normalized) }
}

export function fromBlogContentContract(value: string, label: string): BlogContent {
  const result = parseBlogContent(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toBlogContentContract(value: BlogContent): string {
  return blogContentValue(value)
}
