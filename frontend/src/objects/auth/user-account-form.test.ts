import { describe, expect, it } from 'vitest'

import { validateUserAccountDraft } from '@/objects/auth/user-account-form'

const messages = {
  confirmNewPassword: 'Confirm new password',
  passwordMismatch: 'New passwords do not match.',
}

describe('user-account-form', () => {
  it('requires the current password when updating the signed-in account', () => {
    expect(
      validateUserAccountDraft(
        {
          email: 'alice@example.com',
          currentPassword: '',
          newPassword: '',
          confirmNewPassword: '',
        },
        true,
        messages,
      ),
    ).toEqual({
      ok: false,
      message: 'Password is required.',
    })
  })

  it('builds a managed account update without a current password', () => {
    expect(
      validateUserAccountDraft(
        {
          email: ' alice@example.com ',
          currentPassword: '',
          newPassword: '',
          confirmNewPassword: '',
        },
        false,
        messages,
      ),
    ).toEqual({
      ok: true,
      request: {
        email: 'alice@example.com',
        newPassword: null,
      },
    })
  })

  it('rejects incomplete or mismatched new passwords', () => {
    expect(
      validateUserAccountDraft(
        {
          email: 'alice@example.com',
          currentPassword: 'current',
          newPassword: 'next',
          confirmNewPassword: '',
        },
        true,
        messages,
      ),
    ).toEqual({
      ok: false,
      message: messages.confirmNewPassword,
    })

    expect(
      validateUserAccountDraft(
        {
          email: 'alice@example.com',
          currentPassword: 'current',
          newPassword: 'next',
          confirmNewPassword: 'different',
        },
        true,
        messages,
      ),
    ).toEqual({
      ok: false,
      message: messages.passwordMismatch,
    })
  })
})
