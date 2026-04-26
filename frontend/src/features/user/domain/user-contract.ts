import type {
  AuthUserListItem as AuthUserListItemContract,
  UserListRequest as UserListRequestContract,
  UserListResponse as UserListResponseContract,
  UpdateManagedUserAccountRequest as UpdateManagedUserAccountRequestContract,
  UpdateManagedUserPreferencesRequest as UpdateManagedUserPreferencesRequestContract,
  UpdateManagedUserProfileRequest as UpdateManagedUserProfileRequestContract,
  UpdateOwnAccountRequest as UpdateOwnAccountRequestContract,
  UpdateOwnPreferencesRequest as UpdateOwnPreferencesRequestContract,
  UpdateOwnProfileRequest as UpdateOwnProfileRequestContract,
  UpdateUserPermissionsRequest as UpdateUserPermissionsRequestContract,
  UserAcceptedRanklistItem as UserAcceptedRanklistItemContract,
  UserProfileResponse as UserProfileResponseContract,
  UserRanklistItem as UserRanklistItemContract,
} from '@contracts/auth'
import type { PageResponse as PageResponseContract } from '@contracts/shared'
import type { AuthUserListItem } from '@/features/user/model/AuthUserListItem'
import type { UserListRequest } from '@/features/user/model/UserListRequest'
import type { UserListResponse } from '@/features/user/model/UserListResponse'
import type { UpdateManagedUserAccountRequest } from '@/features/user/model/UpdateManagedUserAccountRequest'
import type { UpdateManagedUserPreferencesRequest } from '@/features/user/model/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/features/user/model/UpdateManagedUserProfileRequest'
import type { UpdateOwnAccountRequest } from '@/features/user/model/UpdateOwnAccountRequest'
import type { UpdateOwnPreferencesRequest } from '@/features/user/model/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/features/user/model/UpdateOwnProfileRequest'
import type { UpdateUserPermissionsRequest } from '@/features/user/model/UpdateUserPermissionsRequest'
import type { UserAcceptedRanklistItem } from '@/features/user/model/UserAcceptedRanklistItem'
import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { UserProfileResponse } from '@/features/user/model/UserProfileResponse'
import type { UserRanklistItem } from '@/features/user/model/UserRanklistItem'
import type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/user/domain/user-responses'
import { parseProblemSlug, parseProblemTitle } from '@/features/problem/domain/problem'
import {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parseUserContribution,
  parseUsername,
  plaintextPasswordValue,
  problemTitleDisplayModeValue,
  requireParsed,
  userDisplayModeValue,
  userLocaleValue,
} from '@/features/user/domain/user-parsers'

export function fromUserIdentityContract(response: {
  username: string
  displayName: string
}): UserIdentity {
  return {
    username: requireParsed(parseUsername(response.username), 'user identity username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'user identity display name'),
  }
}

export function fromUserProfileResponseContract(response: UserProfileResponseContract): UserProfileResponse {
  const acceptedProblems = Array.isArray(response.acceptedProblems) ? response.acceptedProblems : []

  return {
    username: requireParsed(parseUsername(response.username), 'user profile username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'user profile display name'),
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

export function fromUserListResponseContract(response: UserListResponseContract): UserListResponse {
  if (!Array.isArray(response.items)) {
    throw new Error('Invalid user list payload.')
  }

  return {
    items: response.items.map(fromAuthUserListItemContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toUserListRequestContract(request: UserListRequest): UserListRequestContract {
  return {
    query: request.query,
    page: request.page,
    pageSize: request.pageSize,
  }
}

export function toUpdateUserPermissionsRequestContract(
  request: UpdateUserPermissionsRequest,
): UpdateUserPermissionsRequestContract {
  return request
}

export function toUpdateOwnProfileRequestContract(
  request: UpdateOwnProfileRequest,
): UpdateOwnProfileRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
  }
}

export function toUpdateOwnPreferencesRequestContract(
  request: UpdateOwnPreferencesRequest,
): UpdateOwnPreferencesRequestContract {
  return {
    preferences: {
      displayMode: userDisplayModeValue(request.preferences.displayMode),
      locale: userLocaleValue(request.preferences.locale),
      problemTitleDisplayMode: problemTitleDisplayModeValue(request.preferences.problemTitleDisplayMode),
    },
  }
}

export function toUpdateOwnAccountRequestContract(
  request: UpdateOwnAccountRequest,
): UpdateOwnAccountRequestContract {
  return {
    email: emailAddressValue(request.email),
    currentPassword: plaintextPasswordValue(request.currentPassword),
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}

export function toUpdateManagedUserProfileRequestContract(
  request: UpdateManagedUserProfileRequest,
): UpdateManagedUserProfileRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
  }
}

export function toUpdateManagedUserPreferencesRequestContract(
  request: UpdateManagedUserPreferencesRequest,
): UpdateManagedUserPreferencesRequestContract {
  return {
    preferences: {
      displayMode: userDisplayModeValue(request.preferences.displayMode),
      locale: userLocaleValue(request.preferences.locale),
      problemTitleDisplayMode: problemTitleDisplayModeValue(request.preferences.problemTitleDisplayMode),
    },
  }
}

export function toUpdateManagedUserAccountRequestContract(
  request: UpdateManagedUserAccountRequest,
): UpdateManagedUserAccountRequestContract {
  return {
    email: emailAddressValue(request.email),
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}
