import { describe, expect, it } from 'vitest'

import { parseUserSearchQuery, userSearchQueryValue } from '@/features/user/domain/user'

describe('user-parsers', () => {
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
