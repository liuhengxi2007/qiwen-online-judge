import type { ErrorResponse as ErrorResponseContract } from '@contracts/shared'

export type ErrorResponse = ErrorResponseContract

export type { ParseResult } from '@/features/auth/domain/auth-parsers'
export {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  parseUsername,
  plaintextPasswordValue,
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
  fromUserIdentityContract,
  toLoginRequestContract,
  toRegisterRequestContract,
  toUpdateManagedUserSettingsRequestContract,
  toUpdateOwnSettingsRequestContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/auth/domain/auth-contract'

export type { AuthUserListItem } from '@/features/auth/model/AuthUserListItem'
export type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'
export type { LoginRequest } from '@/features/auth/model/LoginRequest'
export type { LoginResponse } from '@/features/auth/model/LoginResponse'
export type { RegisterRequest } from '@/features/auth/model/RegisterRequest'
export type { RegisterResponse } from '@/features/auth/model/RegisterResponse'
export type { SessionResponse } from '@/features/auth/model/SessionResponse'
export type { UserIdentity } from '@/features/auth/model/UserIdentity'
export type { UpdateManagedUserSettingsRequest } from '@/features/auth/model/UpdateManagedUserSettingsRequest'
export type { UpdateOwnSettingsRequest } from '@/features/auth/model/UpdateOwnSettingsRequest'
export type { UpdateUserPermissionsRequest } from '@/features/auth/model/UpdateUserPermissionsRequest'
