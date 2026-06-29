import { describe, expect, it } from 'vitest'

import { validateProblemUpdateDraft } from './ProblemForm'

const baseDraft = {
  title: 'Sample Problem',
  statement: 'Solve it.',
  authorUsername: '',
  baseAccess: 'restricted' as const,
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
  otherUserSubmissionAccess: 'none' as const,
}

describe('problem-update-form', () => {
  it('sends null author username when the author input is empty', () => {
    const result = validateProblemUpdateDraft({ ...baseDraft, authorUsername: '   ' })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.authorUsername).toBeNull()
  })

  it('normalizes author username when present', () => {
    const result = validateProblemUpdateDraft({ ...baseDraft, authorUsername: ' Alice_01 ' })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.authorUsername).toBe('alice_01')
  })
})
