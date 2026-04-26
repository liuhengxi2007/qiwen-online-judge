import type { ErrorResponse as ErrorResponseContract } from '@contracts/shared'

export type ErrorResponse = ErrorResponseContract

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
  fromAuthUserListItemContract,
  fromLoginResponseContract,
  fromRegisterResponseContract,
  fromSessionResponseContract,
  fromUserAcceptedRanklistResponseContract,
  fromUserProfileResponseContract,
  fromUserRanklistResponseContract,
  fromUserIdentityContract,
  toLoginRequestContract,
  toRegisterRequestContract,
  toUpdateManagedUserAccountRequestContract,
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnAccountRequestContract,
  toUpdateOwnPreferencesRequestContract,
  toUpdateOwnProfileRequestContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/auth/domain/auth-contract'

export type { AuthUserListItem } from '@/features/user/model/AuthUserListItem'
export type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'
export type { UserAcceptedProblem } from '@/features/user/model/UserAcceptedProblem'
export type { UserAcceptedRanklistItem } from '@/features/user/model/UserAcceptedRanklistItem'
export type { UserLocale } from '@/features/user/model/UserLocale'
export type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
export type { UserPreferences } from '@/features/user/model/UserPreferences'
export type { UserContribution } from '@/features/user/model/UserContribution'
export type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
export type { LoginRequest } from '@/features/auth/model/LoginRequest'
export type { LoginResponse } from '@/features/auth/model/LoginResponse'
export type { RegisterRequest } from '@/features/auth/model/RegisterRequest'
export type { RegisterResponse } from '@/features/auth/model/RegisterResponse'
export type { SessionResponse } from '@/features/auth/model/SessionResponse'
export type { UserIdentity } from '@/features/user/model/UserIdentity'
export type { UserProfileResponse } from '@/features/user/model/UserProfileResponse'
export type { UserRanklistItem } from '@/features/user/model/UserRanklistItem'
export type { UpdateManagedUserAccountRequest } from '@/features/user/model/UpdateManagedUserAccountRequest'
export type { UpdateManagedUserPreferencesRequest } from '@/features/user/model/UpdateManagedUserPreferencesRequest'
export type { UpdateManagedUserProfileRequest } from '@/features/user/model/UpdateManagedUserProfileRequest'
export type { UpdateOwnAccountRequest } from '@/features/user/model/UpdateOwnAccountRequest'
export type { UpdateOwnPreferencesRequest } from '@/features/user/model/UpdateOwnPreferencesRequest'
export type { UpdateOwnProfileRequest } from '@/features/user/model/UpdateOwnProfileRequest'
export type { UpdateUserPermissionsRequest } from '@/features/user/model/UpdateUserPermissionsRequest'
export type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/user/domain/user'
