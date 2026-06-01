import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import {
  fromProblemTitleDisplayModeContract,
  toProblemTitleDisplayModeContract,
} from '@/objects/problem/ProblemTitleDisplayMode'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { fromUserDisplayModeContract, toUserDisplayModeContract } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import { fromUserLocaleContract, toUserLocaleContract } from '@/objects/user/UserLocale'
import { readBoolean, readRecord, readString } from '@/objects/shared/PageResponse'

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
  value: unknown,
  labelPrefix: string,
): UserPreferences {
  const preferences = readRecord(value, labelPrefix) as UserPreferencesContract
  return {
    displayMode: fromUserDisplayModeContract(
      readString(preferences.displayMode, `${labelPrefix} display mode`),
      `${labelPrefix} display mode`,
    ),
    locale: fromUserLocaleContract(readString(preferences.locale, `${labelPrefix} locale`), `${labelPrefix} locale`),
    problemTitleDisplayMode: fromProblemTitleDisplayModeContract(
      readString(preferences.problemTitleDisplayMode, `${labelPrefix} problem title display mode`),
      `${labelPrefix} problem title display mode`,
    ),
    autoMarkMessageRead: readBoolean(preferences.autoMarkMessageRead, `${labelPrefix} auto mark message read`),
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
