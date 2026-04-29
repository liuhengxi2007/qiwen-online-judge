import { describe, expect, it } from 'vitest'

import {
  parseAddUserGroupMemberRole,
  parseUserGroupDescription,
  parseUserGroupRole,
  parseUserGroupSlug,
  userGroupDescriptionValue,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup-parsers'

describe('usergroup-parsers', () => {
  it('parses trimmed user group slugs', () => {
    const parsed = parseUserGroupSlug('  sample-group  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(userGroupSlugValue(parsed.value)).toBe('sample-group')
    }
  })

  it('rejects malformed user group slugs', () => {
    expect(parseUserGroupSlug('sample group')).toEqual({
      ok: false,
      error: 'User group slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('parses descriptions and roles', () => {
    const description = parseUserGroupDescription('  practice team  ')

    expect(description.ok).toBe(true)
    if (description.ok) {
      expect(userGroupDescriptionValue(description.value)).toBe('practice team')
    }
    expect(parseUserGroupRole('manager')).toEqual({ ok: true, value: 'manager' })
    expect(parseAddUserGroupMemberRole('owner')).toEqual({
      ok: false,
      error: 'New members may only be added as member or manager.',
    })
  })
})
