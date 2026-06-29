import { describe, expect, it } from 'vitest'

import { validateBlogFormDraft } from './BlogForm'

describe('blog-form', () => {
  it('builds viewer-only blog visibility policies', () => {
    const result = validateBlogFormDraft({
      title: 'Shared blog',
      content: 'A useful writeup.',
      baseAccess: 'restricted',
      grantedUsersInput: 'alice',
      grantedGroupsInput: 'reviewers',
    })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.visibilityPolicy).toMatchObject({
      baseAccess: 'restricted',
      viewerGrants: [
        { kind: 'user_group', slug: 'reviewers' },
        { kind: 'user', username: 'alice' },
      ],
    })
    expect('managerGrants' in result.request.visibilityPolicy).toBe(false)
  })
})
