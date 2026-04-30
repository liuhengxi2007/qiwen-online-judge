import {
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  parseProblemTitleDisplayMode,
  parseUserDisplayMode,
  parseUserLocale,
  type ProblemTitleDisplayMode,
  type UpdateManagedUserAccountRequest,
  type UpdateManagedUserPreferencesRequest,
  type UpdateManagedUserProfileRequest,
  type UpdateOwnAccountRequest,
  type UpdateOwnPreferencesRequest,
  type UpdateOwnProfileRequest,
  type UserDisplayMode,
  type UserLocale,
} from '@/features/auth/domain/auth'
import { translateMessage } from '@/shared/i18n/messages'

export type UserProfileDraft = {
  displayName: string
}

export type UserPreferencesDraft = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

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

export function validateUserProfileDraft(
  draft: UserProfileDraft,
): ValidationSuccess<UpdateOwnProfileRequest | UpdateManagedUserProfileRequest> | ValidationFailure {
  const displayNameResult = parseDisplayName(draft.displayName)
  if (!displayNameResult.ok) {
    return { ok: false, message: displayNameResult.error }
  }

  return {
    ok: true,
    request: {
      displayName: displayNameResult.value,
    },
  }
}

export function validateUserPreferencesDraft(
  draft: UserPreferencesDraft,
): ValidationSuccess<UpdateOwnPreferencesRequest | UpdateManagedUserPreferencesRequest> | ValidationFailure {
  const displayModeResult = parseUserDisplayMode(draft.displayMode)
  if (!displayModeResult.ok) {
    return { ok: false, message: displayModeResult.error }
  }

  const localeResult = parseUserLocale(draft.locale)
  if (!localeResult.ok) {
    return { ok: false, message: localeResult.error }
  }

  const problemTitleDisplayModeResult = parseProblemTitleDisplayMode(draft.problemTitleDisplayMode)
  if (!problemTitleDisplayModeResult.ok) {
    return { ok: false, message: problemTitleDisplayModeResult.error }
  }

  return {
    ok: true,
    request: {
      preferences: {
        displayMode: displayModeResult.value,
        locale: localeResult.value,
        problemTitleDisplayMode: problemTitleDisplayModeResult.value,
        autoMarkMessageRead: draft.autoMarkMessageRead,
      },
    },
  }
}

export function validateUserAccountDraft(
  draft: UserAccountDraft,
  isEditingOwnSettings: boolean,
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
        message: confirmNewPasswordResult?.ok
          ? translateMessage('userSettings.passwordMismatch')
          : translateMessage('userSettings.confirmNewPassword'),
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
