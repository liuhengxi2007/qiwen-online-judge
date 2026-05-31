import { describe, expect, it } from 'vitest'

import { validateProblemSetDraft } from './ProblemSetForm'

describe('problem-set-form', () => {
  it('builds problem set access policies without manager grants', () => {
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
      managerGrants: [],
    })
  })
})
