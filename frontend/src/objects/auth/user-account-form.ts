import { parseEmailAddress, parsePlaintextPassword } from '@/objects/auth/auth-parsers'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'

export type UserAccountDraft = {
  email: string
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

type ValidationSuccess<T> = {
  ok: true
  request: T
}

type ValidationFailure = {
  ok: false
  message: string
}

type UserAccountValidationMessages = {
  confirmNewPassword: string
  passwordMismatch: string
}

export function validateUserAccountDraft(
  draft: UserAccountDraft,
  isEditingOwnSettings: boolean,
  messages: UserAccountValidationMessages,
): ValidationSuccess<UpdateOwnAccountRequest | UpdateManagedUserAccountRequest> | ValidationFailure {
  const emailResult = parseEmailAddress(draft.email)
  if (!emailResult.ok) {
    return { ok: false, message: emailResult.error }
  }

  const newPasswordResult = draft.newPassword.trim() ? parsePlaintextPassword(draft.newPassword) : null
  if (newPasswordResult && !newPasswordResult.ok) {
    return { ok: false, message: newPasswordResult.error }
  }

  const confirmNewPasswordResult = draft.confirmNewPassword.trim()
    ? parsePlaintextPassword(draft.confirmNewPassword)
    : null
  if (confirmNewPasswordResult && !confirmNewPasswordResult.ok) {
    return { ok: false, message: confirmNewPasswordResult.error }
  }

  if (newPasswordResult || confirmNewPasswordResult) {
    if (
      !newPasswordResult ||
      !confirmNewPasswordResult ||
      newPasswordResult.value !== confirmNewPasswordResult.value
    ) {
      return {
        ok: false,
        message: confirmNewPasswordResult?.ok ? messages.passwordMismatch : messages.confirmNewPassword,
      }
    }
  }

  if (isEditingOwnSettings) {
    const currentPasswordResult = parsePlaintextPassword(draft.currentPassword)
    if (!currentPasswordResult.ok) {
      return { ok: false, message: currentPasswordResult.error }
    }

    return {
      ok: true,
      request: {
        email: emailResult.value,
        currentPassword: currentPasswordResult.value,
        newPassword: newPasswordResult ? newPasswordResult.value : null,
      },
    }
  }

  return {
    ok: true,
    request: {
      email: emailResult.value,
      newPassword: newPasswordResult ? newPasswordResult.value : null,
    },
  }
}
