import { describe, expect, it } from 'vitest'

import { blogCommentContentValue, parseBlogCommentContent } from '@/objects/blog/BlogCommentContent'
import { parseBlogCommentId } from '@/objects/blog/BlogCommentId'
import { parseBlogContent } from '@/objects/blog/BlogContent'
import { blogIdValue, parseBlogId } from '@/objects/blog/BlogId'
import { parseBlogTitle } from '@/objects/blog/BlogTitle'

describe('blog-parsers', () => {
  it('accepts positive integer blog ids', () => {
    const parsed = parseBlogId(42)

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(blogIdValue(parsed.value)).toBe(42)
    }
  })

  it('rejects non-positive blog ids', () => {
    expect(parseBlogId(0)).toEqual({
      ok: false,
      error: 'Blog id must be a positive integer.',
    })
    expect(parseBlogId(-1)).toEqual({
      ok: false,
      error: 'Blog id must be a positive integer.',
    })
    expect(parseBlogId(1.5)).toEqual({
      ok: false,
      error: 'Blog id must be a positive integer.',
    })
  })

  it('validates blog comment ids with the same integer rules', () => {
    expect(parseBlogCommentId(7)).toEqual({ ok: true, value: 7 })
    expect(parseBlogCommentId(0)).toEqual({
      ok: false,
      error: 'Blog comment id must be a positive integer.',
    })
  })

  it('trims and validates blog titles', () => {
    expect(parseBlogTitle('  Hello World  ')).toEqual({
      ok: true,
      value: 'Hello World',
    })
    expect(parseBlogTitle('   ')).toEqual({
      ok: false,
      error: 'Blog title is required.',
    })
    expect(parseBlogTitle('x'.repeat(161))).toEqual({
      ok: false,
      error: 'Blog title must be at most 160 characters.',
    })
  })

  it('validates blog content boundaries', () => {
    expect(parseBlogContent('   ')).toEqual({
      ok: false,
      error: 'Blog content is required.',
    })
    expect(parseBlogContent('x'.repeat(200001))).toEqual({
      ok: false,
      error: 'Blog content must be at most 200000 characters.',
    })
    expect(parseBlogContent('x'.repeat(200000))).toEqual({
      ok: true,
      value: 'x'.repeat(200000),
    })
  })

  it('trims and validates comment content', () => {
    const parsed = parseBlogCommentContent('  nice post  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(blogCommentContentValue(parsed.value)).toBe('nice post')
    }
    expect(parseBlogCommentContent('   ')).toEqual({
      ok: false,
      error: 'Comment content is required.',
    })
    expect(parseBlogCommentContent('x'.repeat(20001))).toEqual({
      ok: false,
      error: 'Comment content must be at most 20000 characters.',
    })
  })
})
