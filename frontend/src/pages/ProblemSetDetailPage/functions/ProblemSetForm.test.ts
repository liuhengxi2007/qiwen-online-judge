import { describe, expect, it } from 'vitest'

import { validateProblemSetUpdateDraft } from './ProblemSetForm'

const baseDraft = {
  title: 'Sample Set',
  description: 'Practice set.',
  authorUsername: '',
  baseAccess: 'restricted' as const,
  grantedUsersInput: '',
  grantedGroupsInput: '',
}

describe('problem-set-update-form', () => {
  it('sends null author username when the author input is empty', () => {
    const result = validateProblemSetUpdateDraft({ ...baseDraft, authorUsername: '   ' })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.authorUsername).toBeNull()
  })

  it('normalizes author username when present', () => {
    const result = validateProblemSetUpdateDraft({ ...baseDraft, authorUsername: ' Alice_01 ' })

    expect(result.ok).toBe(true)
    if (!result.ok) {
      return
    }

    expect(result.request.authorUsername).toBe('alice_01')
  })
})
