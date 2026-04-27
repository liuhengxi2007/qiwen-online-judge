export type { ParseResult } from '@/features/auth/domain/auth-parsers'
export {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parseProblemTitleDisplayMode,
  parseUserContribution,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  plaintextPasswordValue,
  problemTitleDisplayModeValue,
  requireParsed,
  userContributionValue,
  userDisplayModeValue,
  userLocaleValue,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'
import type { ParseResult } from '@/features/auth/domain/auth-parsers'
import type { UserSearchQuery } from '@/features/user/model/UserSearchQuery'

function createUserSearchQuery(value: string): UserSearchQuery {
  return value as UserSearchQuery
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
