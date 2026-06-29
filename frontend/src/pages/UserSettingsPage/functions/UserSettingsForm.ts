import { parseProblemTitleDisplayMode, type ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { parseDisplayName } from '@/objects/user/DisplayName'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import { parseUserDisplayMode, type UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { parseUserLocale, type UserLocale } from '@/objects/user/UserLocale'

/**
 * 用户资料设置草稿，保存显示名输入。
 */
export type UserProfileDraft = {
  displayName: string
}

/**
 * 用户偏好设置草稿，保存显示模式、语言、题名展示模式和消息已读偏好。
 */
export type UserPreferencesDraft = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

/**
 * 用户资料/偏好校验成功结果，携带可提交请求体。
 */
type ValidationSuccess<T> = {
  ok: true
  request: T
}

/**
 * 用户资料/偏好校验失败结果，携带用户可见错误。
 */
type ValidationFailure = {
  ok: false
  message: string
}

/**
 * 校验用户资料草稿，成功时构造资料更新请求。
 */
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

/**
 * 校验用户偏好草稿，成功时构造偏好更新请求。
 */
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
