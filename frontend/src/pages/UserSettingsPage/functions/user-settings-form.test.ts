import { describe, expect, it } from 'vitest'

import { validateUserPreferencesDraft, validateUserProfileDraft } from './user-settings-form'

describe('user-settings-form', () => {
  it('builds a profile update from a valid display name', () => {
    expect(validateUserProfileDraft({ displayName: ' Alice ' })).toEqual({
      ok: true,
      request: {
        displayName: 'Alice',
      },
    })
  })

  it('builds a preferences update from valid enum values', () => {
    expect(
      validateUserPreferencesDraft({
        displayMode: 'display_name_with_username',
        locale: 'zh-CN',
        problemTitleDisplayMode: 'title_with_slug',
        autoMarkMessageRead: true,
      }),
    ).toEqual({
      ok: true,
      request: {
        preferences: {
          displayMode: 'display_name_with_username',
          locale: 'zh-CN',
          problemTitleDisplayMode: 'title_with_slug',
          autoMarkMessageRead: true,
        },
      },
    })
  })

  it('returns parser errors for invalid user settings drafts', () => {
    expect(validateUserProfileDraft({ displayName: '   ' })).toEqual({
      ok: false,
      message: 'Display name is required.',
    })

    expect(
      validateUserPreferencesDraft({
        displayMode: 'nickname' as never,
        locale: 'en',
        problemTitleDisplayMode: 'title',
        autoMarkMessageRead: false,
      }),
    ).toEqual({
      ok: false,
      message: 'Display mode must be one of: display_name, username, display_name_with_username.',
    })
  })
})
