import { describe, expect, it } from 'vitest'

import { validateProblemSetDraft } from './ProblemSetForm'

describe('problem-set-form', () => {
  it('builds viewer-only problem set visibility policies', () => {
    const result = validateProblemSetDraft({
      slug: 'sample-set',
      title: 'Sample Set',
      description: 'A focused practice set.',
      baseAccess: 'restricted',
      grantedUsersInput: 'alice',
      grantedGroupsInput: 'reviewers',
    })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.accessPolicy).toMatchObject({
      baseAccess: 'restricted',
      viewerGrants: [
        { kind: 'user_group', slug: 'reviewers' },
        { kind: 'user', username: 'alice' },
      ],
    })
    expect('managerGrants' in result.request.accessPolicy).toBe(false)
  })
})
