import { describe, expect, it } from 'vitest'

import {
  displayNameValue,
  parseDisplayName,
  parseProblemTitleDisplayMode,
  parseUserContribution,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  parseUserSearchQuery,
  userSearchQueryValue,
  usernameValue,
} from '@/features/user/domain/user'

describe('user-parsers', () => {
  it('parses usernames by trimming and lowercasing them', () => {
    const parsed = parseUsername('  Alice_01  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(usernameValue(parsed.value)).toBe('alice_01')
    }
  })

  it('rejects usernames with unsupported characters', () => {
    expect(parseUsername('alice!')).toEqual({
      ok: false,
      error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.',
    })
  })

  it('accepts usernames at the exact length bounds and rejects out-of-range lengths', () => {
    expect(parseUsername('abc')).toEqual({ ok: true, value: 'abc' })
    expect(parseUsername('a'.repeat(32))).toEqual({ ok: true, value: 'a'.repeat(32) })
    expect(parseUsername('ab')).toEqual({
      ok: false,
      error: 'Username must be between 3 and 32 characters.',
    })
    expect(parseUsername('a'.repeat(33))).toEqual({
      ok: false,
      error: 'Username must be between 3 and 32 characters.',
    })
  })

  it('parses display names by trimming them', () => {
    const parsed = parseDisplayName('  Alice  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(displayNameValue(parsed.value)).toBe('Alice')
    }
  })

  it('rejects invalid display names', () => {
    expect(parseDisplayName('   ')).toEqual({
      ok: false,
      error: 'Display name is required.',
    })
    expect(parseDisplayName('x'.repeat(121))).toEqual({
      ok: false,
      error: 'Display name must be at most 120 characters.',
    })
  })

  it('rejects unsupported user locales and display modes', () => {
    expect(parseUserLocale('fr')).toEqual({
      ok: false,
      error: 'Locale must be one of: en, zh-CN.',
    })
    expect(parseUserDisplayMode('nickname')).toEqual({
      ok: false,
      error: 'Display mode must be one of: display_name, username, display_name_with_username.',
    })
  })

  it('accepts all supported user locale and display mode enums', () => {
    expect(parseUserLocale('en')).toEqual({ ok: true, value: 'en' })
    expect(parseUserLocale('zh-CN')).toEqual({ ok: true, value: 'zh-CN' })
    expect(parseUserDisplayMode('display_name')).toEqual({ ok: true, value: 'display_name' })
    expect(parseUserDisplayMode('username')).toEqual({ ok: true, value: 'username' })
    expect(parseUserDisplayMode('display_name_with_username')).toEqual({
      ok: true,
      value: 'display_name_with_username',
    })
    expect(parseProblemTitleDisplayMode('title')).toEqual({ ok: true, value: 'title' })
    expect(parseProblemTitleDisplayMode('slug')).toEqual({ ok: true, value: 'slug' })
    expect(parseProblemTitleDisplayMode('title_with_slug')).toEqual({
      ok: true,
      value: 'title_with_slug',
    })
  })

  it('validates user contribution as a finite number', () => {
    expect(parseUserContribution(Number.NaN)).toEqual({
      ok: false,
      error: 'User contribution must be a finite number.',
    })
    expect(parseUserContribution(Number.POSITIVE_INFINITY)).toEqual({
      ok: false,
      error: 'User contribution must be a finite number.',
    })
    expect(parseUserContribution(0)).toEqual({ ok: true, value: 0 })
    expect(parseUserContribution(-1)).toEqual({ ok: true, value: -1 })
  })

  it('parses trimmed user search queries', () => {
    const parsed = parseUserSearchQuery('  alice  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(userSearchQueryValue(parsed.value)).toBe('alice')
    }
  })

  it('preserves internal spaces in user search queries', () => {
    expect(parseUserSearchQuery('  alice bob  ')).toEqual({
      ok: true,
      value: 'alice bob',
    })
  })

  it('rejects blank user search queries', () => {
    expect(parseUserSearchQuery('   ')).toEqual({
      ok: false,
      error: 'User search query is required.',
    })
  })
})
