import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogTitle } from '@/objects/blog/BlogTitle'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
export type ParseResult<T> = ParseSuccess<T> | ParseFailure

function createBlogId(value: number): BlogId {
  return value as BlogId
}

function createBlogTitle(value: string): BlogTitle {
  return value as BlogTitle
}

function createBlogContent(value: string): BlogContent {
  return value as BlogContent
}

function createBlogCommentId(value: number): BlogCommentId {
  return value as BlogCommentId
}

function createBlogCommentContent(value: string): BlogCommentContent {
  return value as BlogCommentContent
}

export function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function blogIdValue(id: BlogId): number {
  return id
}

export function blogTitleValue(title: BlogTitle): string {
  return title
}

export function blogContentValue(content: BlogContent): string {
  return content
}

export function blogCommentIdValue(id: BlogCommentId): number {
  return id
}

export function blogCommentContentValue(content: BlogCommentContent): string {
  return content
}

export function parseBlogId(rawId: number): ParseResult<BlogId> {
  if (!Number.isInteger(rawId) || rawId <= 0) {
    return { ok: false, error: 'Blog id must be a positive integer.' }
  }
  return { ok: true, value: createBlogId(rawId) }
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

export function parseBlogCommentId(rawId: number): ParseResult<BlogCommentId> {
  if (!Number.isInteger(rawId) || rawId <= 0) {
    return { ok: false, error: 'Blog comment id must be a positive integer.' }
  }
  return { ok: true, value: createBlogCommentId(rawId) }
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
