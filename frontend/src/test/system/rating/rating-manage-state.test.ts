import { describe, expect, it } from 'vitest'

import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import {
  initialRatingManagePageState,
  ratingManagePageReducer,
  validateRatingAppendDraft,
} from '@/pages/RatingManagePage/functions/RatingManagePageState'

describe('rating manage page state', () => {
  const emptyState: RatingManageState = { contests: [] }

  it('uses m=60 as the default append value', () => {
    expect(initialRatingManagePageState.draft.mInput).toBe('60')
  })

  it('validates rating m range', () => {
    expect(validateRatingAppendDraft({ contestSlugInput: 'spring-challenge', mInput: '1' })).toEqual({
      ok: false,
      message: 'Rating m must be between 2 and 100.',
    })
    expect(validateRatingAppendDraft({ contestSlugInput: 'spring-challenge', mInput: '101' })).toEqual({
      ok: false,
      message: 'Rating m must be between 2 and 100.',
    })
    expect(validateRatingAppendDraft({ contestSlugInput: 'spring-challenge', mInput: '60' })).toEqual({
      ok: true,
      request: {
        contestSlug: 'spring-challenge',
        m: 60,
      },
    })
  })

  it('tracks append status and resets the draft on success', () => {
    const appending = ratingManagePageReducer(initialRatingManagePageState, { type: 'append_started' })
    expect(appending.isAppending).toBe(true)
    expect(appending.errorMessage).toBe('')

    const appended = ratingManagePageReducer(appending, {
      type: 'append_succeeded',
      state: emptyState,
      message: 'Rating contest appended.',
    })

    expect(appended.isAppending).toBe(false)
    expect(appended.draft).toEqual({ contestSlugInput: '', mInput: '60' })
    expect(appended.manageState).toBe(emptyState)
    expect(appended.noticeMessage).toBe('Rating contest appended.')
  })

  it('tracks pop status', () => {
    const popping = ratingManagePageReducer(initialRatingManagePageState, { type: 'pop_started' })
    expect(popping.isPopping).toBe(true)

    const popped = ratingManagePageReducer(popping, {
      type: 'pop_succeeded',
      state: emptyState,
      message: 'Latest rating contest removed.',
    })

    expect(popped.isPopping).toBe(false)
    expect(popped.manageState).toBe(emptyState)
    expect(popped.noticeMessage).toBe('Latest rating contest removed.')
  })
})
