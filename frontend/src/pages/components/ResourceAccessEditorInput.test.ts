import { describe, expect, it } from 'vitest'

import { buildResourceAccessPolicy, buildResourceVisibilityPolicy } from './ResourceAccessEditorInput'

describe('resource-access-editor-input', () => {
  it('builds branded access subjects from user input', () => {
    const result = buildResourceAccessPolicy({
      baseAccess: 'restricted',
      viewer: { usersInput: 'Alice_01, bob', groupsInput: 'sample-group' },
      manager: { usersInput: 'manager-1', groupsInput: '' },
    })

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
    expect(buildResourceAccessPolicy({
      baseAccess: 'restricted',
      viewer: { usersInput: 'ab', groupsInput: '' },
    })).toEqual({
      ok: false,
      message: 'Username must be between 3 and 32 characters.',
    })
    expect(buildResourceAccessPolicy({
      baseAccess: 'restricted',
      viewer: { usersInput: '', groupsInput: 'bad group' },
    })).toEqual({
      ok: false,
      message: 'User group slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })

  it('builds viewer-only visibility policies', () => {
    const result = buildResourceVisibilityPolicy('public', 'Alice_01', 'sample-group')

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.value).toEqual({
      baseAccess: 'public',
      viewerGrants: [
        { kind: 'user_group', slug: 'sample-group' },
        { kind: 'user', username: 'alice_01' },
      ],
    })
    expect('managerGrants' in result.value).toBe(false)
  })
})
