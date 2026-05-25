import type { DisplayName } from '@/features/user/model/DisplayName'
import type { UserAcceptedProblem } from '@/features/user/model/UserAcceptedProblem'
import type { UserContribution } from '@/features/user/model/UserContribution'
import type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { UserLocale } from '@/features/user/model/UserLocale'
import type { UserPreferences } from '@/features/user/model/UserPreferences'
import type { Username } from '@/features/user/model/Username'
import {
  fromProblemSlugContract,
  fromProblemTitleContract,
  fromProblemTitleDisplayModeContract,
  toProblemTitleDisplayModeContract,
  type ProblemTitleDisplayModeContract,
} from '@/features/problem/http/codec/ProblemModelHttpCodecs'
import {
  displayNameValue,
  parseDisplayName,
  parseUserContribution,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  requireParsed,
  userDisplayModeValue,
  userLocaleValue,
  usernameValue,
} from '@/features/user/lib/user-parsers'

export type UsernameContract = string
export type DisplayNameContract = string
export type UserContributionContract = number
export type UserDisplayModeContract = 'display_name' | 'username' | 'display_name_with_username'
export type UserLocaleContract = 'en' | 'zh-CN'

export type UserIdentityContract = {
  username: UsernameContract
  displayName: DisplayNameContract
}

export type UserPreferencesContract = {
  displayMode: UserDisplayModeContract
  locale: UserLocaleContract
  problemTitleDisplayMode: ProblemTitleDisplayModeContract
  autoMarkMessageRead: boolean
}

export type UserAcceptedProblemContract = {
  slug: string
  title: string
  acceptedAt: string
}

export function fromUsernameContract(value: UsernameContract, label: string): Username {
  return requireParsed(parseUsername(value), label)
}

export function toUsernameContract(value: Username): UsernameContract {
  return usernameValue(value)
}

export function fromDisplayNameContract(value: DisplayNameContract, label: string): DisplayName {
  return requireParsed(parseDisplayName(value), label)
}

export function toDisplayNameContract(value: DisplayName): DisplayNameContract {
  return displayNameValue(value)
}

export function fromUserContributionContract(value: UserContributionContract, label: string): UserContribution {
  return requireParsed(parseUserContribution(value), label)
}

export function fromUserDisplayModeContract(value: UserDisplayModeContract, label: string): UserDisplayMode {
  return requireParsed(parseUserDisplayMode(value), label)
}

export function toUserDisplayModeContract(value: UserDisplayMode): UserDisplayModeContract {
  return userDisplayModeValue(value)
}

export function fromUserLocaleContract(value: UserLocaleContract, label: string): UserLocale {
  return requireParsed(parseUserLocale(value), label)
}

export function toUserLocaleContract(value: UserLocale): UserLocaleContract {
  return userLocaleValue(value)
}

export function fromUserIdentityContract(response: UserIdentityContract): UserIdentity {
  return {
    username: fromUsernameContract(response.username, 'user identity username'),
    displayName: fromDisplayNameContract(response.displayName, 'user identity display name'),
  }
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

export function fromUserAcceptedProblemContract(
  problem: UserAcceptedProblemContract,
  index: number,
): UserAcceptedProblem {
  return {
    slug: fromProblemSlugContract(problem.slug, `user profile accepted problem slug ${index}`),
    title: fromProblemTitleContract(problem.title, `user profile accepted problem title ${index}`),
    acceptedAt: problem.acceptedAt,
  }
}
