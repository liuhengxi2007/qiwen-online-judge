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
  UserAcceptedRanklistItem as UserAcceptedRanklistItemContract,
  UserProfileResponse as UserProfileResponseContract,
  UserRanklistItem as UserRanklistItemContract,
} from '@contracts/auth'
import type { PageResponse as PageResponseContract } from '@contracts/shared'
import type { AuthUserListItem } from '@/features/auth/model/AuthUserListItem'
import type { LoginRequest } from '@/features/auth/model/LoginRequest'
import type { LoginResponse } from '@/features/auth/model/LoginResponse'
import type { RegisterRequest } from '@/features/auth/model/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/model/RegisterResponse'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'
import type { UserAcceptedRanklistItem } from '@/features/auth/model/UserAcceptedRanklistItem'
import type { UserAcceptedRanklistResponse } from '@/features/auth/model/UserAcceptedRanklistResponse'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'
import type { UserProfileResponse } from '@/features/auth/model/UserProfileResponse'
import type { UserRanklistItem } from '@/features/auth/model/UserRanklistItem'
import type { UserRanklistResponse } from '@/features/auth/model/UserRanklistResponse'
import type { UpdateManagedUserSettingsRequest } from '@/features/auth/model/UpdateManagedUserSettingsRequest'
import type { UpdateOwnSettingsRequest } from '@/features/auth/model/UpdateOwnSettingsRequest'
import type { UpdateUserPermissionsRequest } from '@/features/auth/model/UpdateUserPermissionsRequest'
import {
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
  userDisplayModeValue,
  userLocaleValue,
  usernameValue,
} from '@/features/auth/domain/auth-parsers'
import { parseProblemSlug, parseProblemTitle } from '@/features/problem/domain/problem'

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

export function fromUserProfileResponseContract(response: UserProfileResponseContract): UserProfileResponse {
  const acceptedProblems = Array.isArray(response.acceptedProblems) ? response.acceptedProblems : []

  return {
    username: requireParsed(parseUsername(response.username), 'user profile username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'user profile display name'),
    preferences: {
      displayMode: requireParsed(parseUserDisplayMode(response.preferences.displayMode), 'user profile display mode'),
      locale: requireParsed(parseUserLocale(response.preferences.locale), 'user profile locale'),
      problemTitleDisplayMode: requireParsed(
        parseProblemTitleDisplayMode(response.preferences.problemTitleDisplayMode),
        'user profile problem title display mode',
      ),
    },
    contribution: requireParsed(parseUserContribution(response.contribution), 'user profile contribution'),
    acceptedProblems: acceptedProblems.map((problem, index) => ({
      slug: requireParsed(parseProblemSlug(problem.slug), `user profile accepted problem slug ${index}`),
      title: requireParsed(parseProblemTitle(problem.title), `user profile accepted problem title ${index}`),
      acceptedAt: problem.acceptedAt,
    })),
  }
}

export function fromUserRanklistItemContract(response: UserRanklistItemContract): UserRanklistItem {
  return {
    user: fromUserIdentityContract(response.user),
    contribution: requireParsed(parseUserContribution(response.contribution), 'user ranklist contribution'),
  }
}

export function fromUserRanklistResponseContract(
  response: PageResponseContract<UserRanklistItemContract>,
): UserRanklistResponse {
  if (!Array.isArray(response.items)) {
    throw new Error('Invalid user ranklist payload.')
  }

  return {
    items: response.items.map(fromUserRanklistItemContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function fromUserAcceptedRanklistItemContract(response: UserAcceptedRanklistItemContract): UserAcceptedRanklistItem {
  return {
    user: fromUserIdentityContract(response.user),
    acceptedCount: response.acceptedCount,
  }
}

export function fromUserAcceptedRanklistResponseContract(
  response: PageResponseContract<UserAcceptedRanklistItemContract>,
): UserAcceptedRanklistResponse {
  if (!Array.isArray(response.items)) {
    throw new Error('Invalid user accepted ranklist payload.')
  }

  return {
    items: response.items.map(fromUserAcceptedRanklistItemContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
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
