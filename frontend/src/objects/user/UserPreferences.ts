import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import {
  fromProblemTitleDisplayModeContract,
  toProblemTitleDisplayModeContract,
} from '@/objects/problem/ProblemTitleDisplayMode'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { fromUserDisplayModeContract, toUserDisplayModeContract } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import { fromUserLocaleContract, toUserLocaleContract } from '@/objects/user/UserLocale'

export type UserPreferences = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

type UserPreferencesContract = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
}

export function fromUserPreferencesContract(
  preferences: UserPreferencesContract,
  labelPrefix: string,
): UserPreferences {
  return {
    displayMode: fromUserDisplayModeContract(preferences.displayMode, `${labelPrefix} display mode`),
    locale: fromUserLocaleContract(preferences.locale, `${labelPrefix} locale`),
    problemTitleDisplayMode: fromProblemTitleDisplayModeContract(
      preferences.problemTitleDisplayMode,
      `${labelPrefix} problem title display mode`,
    ),
    autoMarkMessageRead: preferences.autoMarkMessageRead,
  }
}

export function toUserPreferencesContract(preferences: UserPreferences): UserPreferencesContract {
  return {
    displayMode: toUserDisplayModeContract(preferences.displayMode),
    locale: toUserLocaleContract(preferences.locale),
    problemTitleDisplayMode: toProblemTitleDisplayModeContract(preferences.problemTitleDisplayMode),
    autoMarkMessageRead: preferences.autoMarkMessageRead,
  }
}
