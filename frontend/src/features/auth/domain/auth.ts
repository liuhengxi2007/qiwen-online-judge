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

export type { AuthUserListItem } from '@/features/user/http/response/AuthUserListItem'
export type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'
export type { UserAcceptedProblem } from '@/features/user/model/UserAcceptedProblem'
export type { UserAcceptedRanklistItem } from '@/features/user/http/response/UserAcceptedRanklistItem'
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
export type { UserIdentity } from '@/features/user/model/UserIdentity'
export type { UserProfileResponse } from '@/features/user/http/response/UserProfileResponse'
export type { UserRanklistItem } from '@/features/user/http/response/UserRanklistItem'
export type { UpdateManagedUserAccountRequest } from '@/features/user/http/request/UpdateManagedUserAccountRequest'
export type { UpdateManagedUserPreferencesRequest } from '@/features/user/http/request/UpdateManagedUserPreferencesRequest'
export type { UpdateManagedUserProfileRequest } from '@/features/user/http/request/UpdateManagedUserProfileRequest'
export type { UpdateOwnAccountRequest } from '@/features/user/http/request/UpdateOwnAccountRequest'
export type { UpdateOwnPreferencesRequest } from '@/features/user/http/request/UpdateOwnPreferencesRequest'
export type { UpdateOwnProfileRequest } from '@/features/user/http/request/UpdateOwnProfileRequest'
export type { UpdateUserPermissionsRequest } from '@/features/user/http/request/UpdateUserPermissionsRequest'
export type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/user/domain/user'
