import { describe, expect, it } from 'vitest'

import { parseNewUserGroupMemberRole } from '@/objects/usergroup/NewUserGroupMemberRole'
import { parseUserGroupDescription, userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { parseUserGroupId } from '@/objects/usergroup/UserGroupId'
import { parseUserGroupName } from '@/objects/usergroup/UserGroupName'
import { parseUserGroupRole } from '@/objects/usergroup/UserGroupRole'
import { parseUserGroupSlug, userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'

const userGroupId = '11111111-1111-4111-8111-111111111111'

describe('usergroup-parsers', () => {
  it('parses trimmed user group slugs', () => {
    const parsed = parseUserGroupSlug('  sample-group  ')

    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(userGroupSlugValue(parsed.value)).toBe('sample-group')
    }
  })

  it('parses valid user group ids and rejects malformed ones', () => {
    expect(parseUserGroupId(` ${userGroupId} `)).toEqual({
      ok: true,
      value: userGroupId,
    })
    expect(parseUserGroupId('bad-id')).toEqual({
      ok: false,
      error: 'User group id must be a valid UUID.',
    })
  })

  it('rejects malformed and out-of-range user group slugs', () => {
    expect(parseUserGroupSlug('')).toEqual({
      ok: false,
      error: 'User group slug is required.',
    })
    expect(parseUserGroupSlug('ab')).toEqual({
      ok: false,
      error: 'User group slug must be between 3 and 64 characters.',
    })
    expect(parseUserGroupSlug('a'.repeat(65))).toEqual({
      ok: false,
      error: 'User group slug must be between 3 and 64 characters.',
    })
    expect(parseUserGroupSlug('sample group')).toEqual({
      ok: false,
      error: 'User group slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('parses descriptions names and roles', () => {
    const description = parseUserGroupDescription('  practice team  ')

    expect(description.ok).toBe(true)
    if (description.ok) {
      expect(userGroupDescriptionValue(description.value)).toBe('practice team')
    }
    expect(parseUserGroupName('  Team Alpha  ')).toEqual({
      ok: true,
      value: 'Team Alpha',
    })
    expect(parseUserGroupName('   ')).toEqual({
      ok: false,
      error: 'User group name is required.',
    })
    expect(parseUserGroupName('x'.repeat(121))).toEqual({
      ok: false,
      error: 'User group name must be at most 120 characters.',
    })
    expect(parseUserGroupDescription('x'.repeat(2001))).toEqual({
      ok: false,
      error: 'User group description must be at most 2000 characters.',
    })
    expect(parseUserGroupDescription('   ')).toEqual({
      ok: true,
      value: '',
    })
    expect(parseUserGroupRole('owner')).toEqual({ ok: true, value: 'owner' })
    expect(parseUserGroupRole('manager')).toEqual({ ok: true, value: 'manager' })
    expect(parseUserGroupRole('member')).toEqual({ ok: true, value: 'member' })
    expect(parseUserGroupRole('guest')).toEqual({ ok: false, error: 'Unknown user group role.' })
    expect(parseNewUserGroupMemberRole('manager')).toEqual({ ok: true, value: 'manager' })
    expect(parseNewUserGroupMemberRole('member')).toEqual({ ok: true, value: 'member' })
    expect(parseNewUserGroupMemberRole('owner')).toEqual({
      ok: false,
      error: 'New members may only be added as member or manager.',
    })
  })
})
