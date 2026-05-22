import type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
import type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
import type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/http/response/RegisterResponse'
import type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
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
  requireParsed,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'

type UserPreferencesContract = {
  displayMode: 'display_name' | 'username' | 'display_name_with_username'
  locale: 'en' | 'zh-CN'
  problemTitleDisplayMode: 'title' | 'slug' | 'title_with_slug'
  autoMarkMessageRead: boolean
}

type LoginRequestContract = {
  username: string
  password: string
}

type LoginResponseContract = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferencesContract
  siteManager: boolean
  problemManager: boolean
  message: string
}

type RegisterRequestContract = {
  username: string
  displayName: string
  email: string
  password: string
}

type RegisterResponseContract = LoginResponseContract

type SessionResponseContract = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferencesContract
  siteManager: boolean
  problemManager: boolean
}

export {
  fromAuthUserListItemContract,
  fromUserAcceptedRanklistResponseContract,
  fromUserIdentityContract,
  fromUserProfileResponseContract,
  fromUserRanklistResponseContract,
  toUpdateManagedUserAccountRequestContract,
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnAccountRequestContract,
  toUpdateOwnPreferencesRequestContract,
  toUpdateOwnProfileRequestContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/user/domain/user-contract'

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
      autoMarkMessageRead: response.preferences.autoMarkMessageRead,
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
      autoMarkMessageRead: response.preferences.autoMarkMessageRead,
    },
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}
