import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { UserContribution } from '@/features/user/model/UserContribution'
import type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
import type { UserLocale } from '@/features/user/model/UserLocale'
import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'
import type { UserSearchQuery } from '@/features/user/model/request/UserSearchQuery'
import type { ParseResult } from '@/shared/domain/parsing'

export type { ParseResult } from '@/shared/domain/parsing'
export { requireParsed } from '@/shared/domain/parsing'

const usernamePattern = /^[a-z0-9_-]+$/

function createUsername(value: string): Username {
  return value as Username
}

function createDisplayName(value: string): DisplayName {
  return value as DisplayName
}

function createUserDisplayMode(value: UserDisplayMode): UserDisplayMode {
  return value
}

function createUserLocale(value: UserLocale): UserLocale {
  return value
}

function createProblemTitleDisplayMode(value: ProblemTitleDisplayMode): ProblemTitleDisplayMode {
  return value
}

function createUserContribution(value: number): UserContribution {
  return value as UserContribution
}

function createUserSearchQuery(value: string): UserSearchQuery {
  return value as UserSearchQuery
}

export function usernameValue(username: Username): string {
  return username
}

export function displayNameValue(displayName: DisplayName): string {
  return displayName
}

export function userDisplayModeValue(displayMode: UserDisplayMode): UserDisplayMode {
  return displayMode
}

export function userLocaleValue(locale: UserLocale): UserLocale {
  return locale
}

export function problemTitleDisplayModeValue(displayMode: ProblemTitleDisplayMode): ProblemTitleDisplayMode {
  return displayMode
}

export function userContributionValue(contribution: UserContribution): number {
  return contribution
}

export function parseUsername(rawUsername: string): ParseResult<Username> {
  const normalized = rawUsername.trim().toLowerCase()

  if (!normalized) {
    return { ok: false, error: 'Username is required.' }
  }

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: createUsername(normalized) }
}

export function parseDisplayName(rawDisplayName: string): ParseResult<DisplayName> {
  const normalized = rawDisplayName.trim()

  if (!normalized) {
    return { ok: false, error: 'Display name is required.' }
  }

  if (normalized.length > 120) {
    return { ok: false, error: 'Display name must be at most 120 characters.' }
  }

  return { ok: true, value: createDisplayName(normalized) }
}

export function parseUserDisplayMode(rawDisplayMode: string): ParseResult<UserDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'display_name':
    case 'username':
    case 'display_name_with_username':
      return { ok: true, value: createUserDisplayMode(normalized) }
    default:
      return {
        ok: false,
        error: 'Display mode must be one of: display_name, username, display_name_with_username.',
      }
  }
}

export function parseUserLocale(rawLocale: string): ParseResult<UserLocale> {
  const normalized = rawLocale.trim()

  switch (normalized) {
    case 'en':
    case 'zh-CN':
      return { ok: true, value: createUserLocale(normalized) }
    default:
      return {
        ok: false,
        error: 'Locale must be one of: en, zh-CN.',
      }
  }
}

export function parseProblemTitleDisplayMode(rawDisplayMode: string): ParseResult<ProblemTitleDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'title':
    case 'slug':
    case 'title_with_slug':
      return { ok: true, value: createProblemTitleDisplayMode(normalized) }
    default:
      return {
        ok: false,
        error: 'Problem title display mode must be one of: title, slug, title_with_slug.',
      }
  }
}

export function parseUserContribution(rawContribution: number): ParseResult<UserContribution> {
  if (!Number.isFinite(rawContribution)) {
    return { ok: false, error: 'User contribution must be a finite number.' }
  }

  return { ok: true, value: createUserContribution(rawContribution) }
}

export function parseUserSearchQuery(rawQuery: string): ParseResult<UserSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'User search query is required.' }
  }
  return { ok: true, value: createUserSearchQuery(normalized) }
}

export function userSearchQueryValue(query: UserSearchQuery): string {
  return query
}
