import type {
  AuthUserListItem as AuthUserListItemContract,
  LoginRequest as LoginRequestContract,
  LoginResponse as LoginResponseContract,
  RegisterRequest as RegisterRequestContract,
  RegisterResponse as RegisterResponseContract,
  SessionResponse as SessionResponseContract,
  UpdateManagedUserSettingsRequest as UpdateManagedUserSettingsRequestContract,
  UpdateOwnSettingsRequest as UpdateOwnSettingsRequestContract,
  UpdateUserPermissionsRequest as UpdateUserPermissionsRequestContract,
} from '@contracts/auth'
import type { AuthUserListItem } from '@/features/auth/model/AuthUserListItem'
import type { LoginRequest } from '@/features/auth/model/LoginRequest'
import type { LoginResponse } from '@/features/auth/model/LoginResponse'
import type { RegisterRequest } from '@/features/auth/model/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/model/RegisterResponse'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'
import type { UpdateManagedUserSettingsRequest } from '@/features/auth/model/UpdateManagedUserSettingsRequest'
import type { UpdateOwnSettingsRequest } from '@/features/auth/model/UpdateOwnSettingsRequest'
import type { UpdateUserPermissionsRequest } from '@/features/auth/model/UpdateUserPermissionsRequest'
import {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parseProblemTitleDisplayMode,
  parseUserDisplayMode,
  parseUserLocale,
  parseUsername,
  plaintextPasswordValue,
  problemTitleDisplayModeValue,
  requireParsed,
  userDisplayModeValue,
  userLocaleValue,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'

export function toLoginRequestContract(request: LoginRequest): LoginRequestContract {
  return {
    username: usernameValue(request.username),
    password: plaintextPasswordValue(request.password),
  }
}

export function fromLoginResponseContract(response: LoginResponseContract): LoginResponse {
  return {
    displayName: requireParsed(parseDisplayName(response.displayName), 'login response display name'),
    username: requireParsed(parseUsername(response.username), 'login response username'),
    email: requireParsed(parseEmailAddress(response.email), 'login response email'),
    preferences: {
      displayMode: requireParsed(parseUserDisplayMode(response.preferences.displayMode), 'login response display mode'),
      locale: requireParsed(parseUserLocale(response.preferences.locale), 'login response locale'),
      problemTitleDisplayMode: requireParsed(
        parseProblemTitleDisplayMode(response.preferences.problemTitleDisplayMode),
        'login response problem title display mode',
      ),
    },
    siteManager: response.siteManager,
    problemManager: response.problemManager,
    message: response.message,
  }
}

export function toRegisterRequestContract(request: RegisterRequest): RegisterRequestContract {
  return {
    username: usernameValue(request.username),
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    password: plaintextPasswordValue(request.password),
  }
}

export function fromRegisterResponseContract(response: RegisterResponseContract): RegisterResponse {
  return fromLoginResponseContract(response)
}

export function fromSessionResponseContract(response: SessionResponseContract): SessionResponse {
  return {
    displayName: requireParsed(parseDisplayName(response.displayName), 'session response display name'),
    username: requireParsed(parseUsername(response.username), 'session response username'),
    email: requireParsed(parseEmailAddress(response.email), 'session response email'),
    preferences: {
      displayMode: requireParsed(parseUserDisplayMode(response.preferences.displayMode), 'session response display mode'),
      locale: requireParsed(parseUserLocale(response.preferences.locale), 'session response locale'),
      problemTitleDisplayMode: requireParsed(
        parseProblemTitleDisplayMode(response.preferences.problemTitleDisplayMode),
        'session response problem title display mode',
      ),
    },
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function fromUserIdentityContract(response: {
  username: string
  displayName: string
  preferences: {
    displayMode: string
    locale: string
    problemTitleDisplayMode: string
  }
}): UserIdentity {
  return {
    username: requireParsed(parseUsername(response.username), 'user identity username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'user identity display name'),
    preferences: {
      displayMode: requireParsed(parseUserDisplayMode(response.preferences.displayMode), 'user identity display mode'),
      locale: requireParsed(parseUserLocale(response.preferences.locale), 'user identity locale'),
      problemTitleDisplayMode: requireParsed(
        parseProblemTitleDisplayMode(response.preferences.problemTitleDisplayMode),
        'user identity problem title display mode',
      ),
    },
  }
}

export function fromAuthUserListItemContract(response: AuthUserListItemContract): AuthUserListItem {
  return {
    username: requireParsed(parseUsername(response.username), 'auth user username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'auth user display name'),
    email: requireParsed(parseEmailAddress(response.email), 'auth user email'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function toUpdateUserPermissionsRequestContract(
  request: UpdateUserPermissionsRequest,
): UpdateUserPermissionsRequestContract {
  return request
}

export function toUpdateOwnSettingsRequestContract(
  request: UpdateOwnSettingsRequest,
): UpdateOwnSettingsRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    preferences: {
      displayMode: userDisplayModeValue(request.preferences.displayMode),
      locale: userLocaleValue(request.preferences.locale),
      problemTitleDisplayMode: problemTitleDisplayModeValue(request.preferences.problemTitleDisplayMode),
    },
    currentPassword: plaintextPasswordValue(request.currentPassword),
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}

export function toUpdateManagedUserSettingsRequestContract(
  request: UpdateManagedUserSettingsRequest,
): UpdateManagedUserSettingsRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    preferences: {
      displayMode: userDisplayModeValue(request.preferences.displayMode),
      locale: userLocaleValue(request.preferences.locale),
      problemTitleDisplayMode: problemTitleDisplayModeValue(request.preferences.problemTitleDisplayMode),
    },
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}
