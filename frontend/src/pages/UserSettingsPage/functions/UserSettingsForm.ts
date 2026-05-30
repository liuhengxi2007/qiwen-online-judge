import { parseProblemTitleDisplayMode, type ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { parseDisplayName } from '@/objects/user/DisplayName'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import { parseUserDisplayMode, type UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { parseUserLocale, type UserLocale } from '@/objects/user/UserLocale'

export type UserProfileDraft = {
  displayName: string
}

export type UserPreferencesDraft = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
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
