import { describe, expect, it } from 'vitest'

import {
  fromResourceAccessPolicyContract,
  toResourceAccessPolicyContract,
} from '@/objects/shared/access/ResourceAccessPolicy'
import { accessUserGroupSlugValue } from '@/objects/shared/access/AccessUserGroupSlug'
import { accessUsernameValue } from '@/objects/shared/access/AccessUsername'

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
    expect(policy.baseAccess).toBe('restricted')
    expect(toResourceAccessPolicyContract(policy)).toEqual({
      baseAccess: 'restricted',
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
        baseAccess: 'restricted',
        viewerGrants: [{ kind: 'user', username: 'ab' }],
        managerGrants: [],
      }),
    ).toThrow('Invalid resource access viewer grant username in contract payload: Username must be between 3 and 32 characters.')

    expect(() =>
      fromResourceAccessPolicyContract({
        baseAccess: 'restricted',
        viewerGrants: [{ kind: 'user_group', slug: 'bad group' }],
        managerGrants: [],
      }),
    ).toThrow('Invalid resource access viewer grant slug in contract payload: User group slug may contain only lowercase letters, numbers, and hyphens.')
  })
})
