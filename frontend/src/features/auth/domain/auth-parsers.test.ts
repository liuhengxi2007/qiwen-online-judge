import { describe, expect, it } from 'vitest'

import {
  displayNameValue,
  parseDisplayName,
  parseProblemTitleDisplayMode,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'

describe('auth-parsers', () => {
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

  it('parses display names by trimming them', () => {
    const parsed = parseDisplayName('  Alice  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(displayNameValue(parsed.value)).toBe('Alice')
    }
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

  it('parses the problem title display mode enum', () => {
    expect(parseProblemTitleDisplayMode('title_with_slug')).toEqual({
      ok: true,
      value: 'title_with_slug',
    })
  })
})
