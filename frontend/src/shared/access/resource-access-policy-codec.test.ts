import { describe, expect, it } from 'vitest'

import {
  fromResourceAccessPolicyContract,
  toResourceAccessPolicyContract,
} from '@/shared/access/resource-access-policy-codec'
import {
  accessUserGroupSlugValue,
  accessUsernameValue,
  resourceAccessSubjectParsers,
} from '@/shared/access/access-subject-parsers'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'

describe('resource access policy codec', () => {
  it('decodes and encodes access subjects through branded domain values', () => {
    const policy = fromResourceAccessPolicyContract({
      baseAccess: 'owner_only',
      viewerGrants: [
        { kind: 'user', username: '  Alice_01  ' },
        { kind: 'user_group', slug: 'sample-group' },
      ],
      managerGrants: [{ kind: 'user', username: 'manager-1' }],
    })

    const viewerUser = policy.viewerGrants.find((grant) => grant.kind === 'user')
    const viewerGroup = policy.viewerGrants.find((grant) => grant.kind === 'user_group')

    expect(viewerUser?.kind === 'user' ? accessUsernameValue(viewerUser.username) : null).toBe('alice_01')
    expect(viewerGroup?.kind === 'user_group' ? accessUserGroupSlugValue(viewerGroup.slug) : null).toBe('sample-group')
    expect(toResourceAccessPolicyContract(policy)).toEqual({
      baseAccess: 'owner_only',
      viewerGrants: [
        { kind: 'user', username: 'alice_01' },
        { kind: 'user_group', slug: 'sample-group' },
      ],
      managerGrants: [{ kind: 'user', username: 'manager-1' }],
    })
  })

  it('rejects invalid access subject contract values', () => {
    expect(() =>
      fromResourceAccessPolicyContract({
        baseAccess: 'owner_only',
        viewerGrants: [{ kind: 'user', username: 'ab' }],
        managerGrants: [],
      }),
    ).toThrow('Invalid resource access viewer grant username in contract payload: Username must be between 3 and 32 characters.')

    expect(() =>
      fromResourceAccessPolicyContract({
        baseAccess: 'owner_only',
        viewerGrants: [{ kind: 'user_group', slug: 'bad group' }],
        managerGrants: [],
      }),
    ).toThrow('Invalid resource access viewer grant slug in contract payload: User group slug may contain only lowercase letters, numbers, and hyphens.')
  })
})

describe('resource access input builder', () => {
  it('builds branded access subjects from user input', () => {
    const result = buildResourceAccessPolicy(
      resourceAccessSubjectParsers,
      'owner_only',
      'Alice_01, bob',
      'sample-group',
      'manager-1',
      '',
    )

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.value.viewerGrants).toEqual([
      { kind: 'user_group', slug: 'sample-group' },
      { kind: 'user', username: 'alice_01' },
      { kind: 'user', username: 'bob' },
    ])
    expect(result.value.managerGrants).toEqual([{ kind: 'user', username: 'manager-1' }])
  })

  it('rejects invalid user and group input before building policies', () => {
    expect(buildResourceAccessPolicy(resourceAccessSubjectParsers, 'owner_only', 'ab', '')).toEqual({
      ok: false,
      message: 'Username must be between 3 and 32 characters.',
    })
    expect(buildResourceAccessPolicy(resourceAccessSubjectParsers, 'owner_only', '', 'bad group')).toEqual({
      ok: false,
      message: 'User group slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })
})
