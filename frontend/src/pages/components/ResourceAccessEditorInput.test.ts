import { describe, expect, it } from 'vitest'

import { buildResourceAccessPolicy } from './ResourceAccessEditorInput'

describe('resource-access-editor-input', () => {
  it('builds branded access subjects from user input', () => {
    const result = buildResourceAccessPolicy(
      'restricted',
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
    expect(buildResourceAccessPolicy('restricted', 'ab', '')).toEqual({
      ok: false,
      message: 'Username must be between 3 and 32 characters.',
    })
    expect(buildResourceAccessPolicy('restricted', '', 'bad group')).toEqual({
      ok: false,
      message: 'User group slug may contain only lowercase letters, numbers, and hyphens.',
    })
  })
})
