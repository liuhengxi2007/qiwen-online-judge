import type { ErrorResponse } from '@/shared/model/ErrorResponse'

export type { ErrorResponse }

export type { ParseResult } from '@/features/auth/domain/auth-parsers'
export {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  parseProblemTitleDisplayMode,
  parseUserContribution,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  plaintextPasswordValue,
  problemTitleDisplayModeValue,
  userContributionValue,
  userDisplayModeValue,
  userLocaleValue,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'
export {
  asProblemManagerSession,
  asSiteManagerSession,
  isProblemManagerSession,
  isSiteManagerSession,
  toAuthSession,
} from '@/features/auth/domain/auth-session'
export {
  fromLoginResponseContract,
  fromRegisterResponseContract,
  fromSessionResponseContract,
  toLoginRequestContract,
  toRegisterRequestContract,
} from '@/features/auth/domain/auth-contract'

export type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'
export type { UserLocale } from '@/features/user/model/UserLocale'
export type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
export type { UserPreferences } from '@/features/user/model/UserPreferences'
export type { UserContribution } from '@/features/user/model/UserContribution'
export type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
export type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
export type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
export type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
export type { RegisterResponse } from '@/features/auth/http/response/RegisterResponse'
export type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
