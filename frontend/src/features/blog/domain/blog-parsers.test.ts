import { describe, expect, it } from 'vitest'

import {
  blogCommentContentValue,
  blogIdValue,
  parseBlogCommentContent,
  parseBlogId,
  parseBlogTitle,
} from '@/features/blog/domain/blog-parsers'

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
  })

  it('trims and validates blog titles', () => {
    expect(parseBlogTitle('  Hello World  ')).toEqual({
      ok: true,
      value: 'Hello World',
    })
  })

  it('trims and validates comment content', () => {
    const parsed = parseBlogCommentContent('  nice post  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(blogCommentContentValue(parsed.value)).toBe('nice post')
    }
  })
})
