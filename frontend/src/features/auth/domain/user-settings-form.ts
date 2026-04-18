import {
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  parseProblemTitleDisplayMode,
  parseUserDisplayMode,
  parseUserLocale,
  type UpdateManagedUserSettingsRequest,
  type UpdateOwnSettingsRequest,
} from '@/features/auth/domain/auth'

export type UserSettingsDraft = {
  displayName: string
  email: string
  displayMode: string
  locale: string
  problemTitleDisplayMode: string
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

export type UserSettingsSubmission =
  | { kind: 'own'; request: UpdateOwnSettingsRequest }
  | { kind: 'managed'; request: UpdateManagedUserSettingsRequest }

type ValidationSuccess = {
  ok: true
  submission: UserSettingsSubmission
}

type ValidationFailure = {
  ok: false
  message: string
}

export function validateUserSettingsDraft(
  draft: UserSettingsDraft,
  isEditingOwnSettings: boolean,
): ValidationSuccess | ValidationFailure {
  const displayNameResult = parseDisplayName(draft.displayName)
  if (!displayNameResult.ok) {
    return { ok: false, message: displayNameResult.error }
  }

  const emailResult = parseEmailAddress(draft.email)
  if (!emailResult.ok) {
    return { ok: false, message: emailResult.error }
  }

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

  const currentPasswordResult =
    isEditingOwnSettings || draft.currentPassword.trim()
      ? parsePlaintextPassword(draft.currentPassword)
      : null

  if (isEditingOwnSettings && (!currentPasswordResult || !currentPasswordResult.ok)) {
    return {
      ok: false,
      message: currentPasswordResult?.error ?? 'Password is required.',
    }
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
      return { ok: false, message: 'New passwords do not match.' }
    }
  }

  const currentPassword = currentPasswordResult && currentPasswordResult.ok ? currentPasswordResult.value : null

  if (isEditingOwnSettings) {
    if (!currentPassword) {
      return { ok: false, message: 'Password is required.' }
    }

    return {
      ok: true,
      submission: {
        kind: 'own',
        request: {
          displayName: displayNameResult.value,
          email: emailResult.value,
          preferences: {
            displayMode: displayModeResult.value,
            locale: localeResult.value,
            problemTitleDisplayMode: problemTitleDisplayModeResult.value,
          },
          currentPassword,
          newPassword: newPasswordResult ? newPasswordResult.value : null,
        },
      },
    }
  }

  return {
    ok: true,
    submission: {
      kind: 'managed',
      request: {
        displayName: displayNameResult.value,
        email: emailResult.value,
        preferences: {
          displayMode: displayModeResult.value,
          locale: localeResult.value,
          problemTitleDisplayMode: problemTitleDisplayModeResult.value,
        },
        newPassword: newPasswordResult ? newPasswordResult.value : null,
      },
    },
  }
}
