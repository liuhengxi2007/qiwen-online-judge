import type { AuthUserListItem } from '@/features/user/http/response/AuthUserListItem'
import type { UserListRequest } from '@/features/user/http/request/UserListRequest'
import type { UserListResponse } from '@/features/user/http/response/UserListResponse'
import type { UpdateManagedUserAccountRequest } from '@/features/user/http/request/UpdateManagedUserAccountRequest'
import type { UpdateManagedUserPreferencesRequest } from '@/features/user/http/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/features/user/http/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnAccountRequest } from '@/features/user/http/request/UpdateOwnAccountRequest'
import type { UpdateOwnPreferencesRequest } from '@/features/user/http/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/features/user/http/request/UpdateOwnProfileRequest'
import type { UpdateUserPermissionsRequest } from '@/features/user/http/request/UpdateUserPermissionsRequest'
import type { UserAcceptedRanklistItem } from '@/features/user/http/response/UserAcceptedRanklistItem'
import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { UserProfileResponse } from '@/features/user/http/response/UserProfileResponse'
import type { UserRanklistItem } from '@/features/user/http/response/UserRanklistItem'
import type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/user/domain/user-responses'
import {
  emailAddressValue,
  parseEmailAddress,
  plaintextPasswordValue,
} from '@/features/auth/domain/auth-parsers'
import { parseProblemSlug, parseProblemTitle } from '@/features/problem/domain/problem'
import {
  displayNameValue,
  parseDisplayName,
  parseUserContribution,
  userSearchQueryValue,
  parseUsername,
  problemTitleDisplayModeValue,
  requireParsed,
  userDisplayModeValue,
  userLocaleValue,
} from '@/features/user/domain/user-parsers'

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type UserIdentityContract = {
  username: string
  displayName: string
}

type UserPreferencesContract = {
  displayMode: 'display_name' | 'username' | 'display_name_with_username'
  locale: 'en' | 'zh-CN'
  problemTitleDisplayMode: 'title' | 'slug' | 'title_with_slug'
  autoMarkMessageRead: boolean
}

type AuthUserListItemContract = {
  username: string
  displayName: string
  email: string
  siteManager: boolean
  problemManager: boolean
}

type UserListRequestContract = {
  query: string | null
  page: number
  pageSize: number
}

type UserAcceptedProblemContract = {
  slug: string
  title: string
  acceptedAt: string
}

type UserProfileResponseContract = {
  username: string
  displayName: string
  contribution: number
  acceptedProblems: UserAcceptedProblemContract[]
}

type UserRanklistItemContract = {
  user: UserIdentityContract
  contribution: number
}

type UserAcceptedRanklistItemContract = {
  user: UserIdentityContract
  acceptedCount: number
}

type UserListResponseContract = PageResponseContract<AuthUserListItemContract>

type UpdateUserPermissionsRequestContract = {
  siteManager: boolean
  problemManager: boolean
}

type UpdateOwnProfileRequestContract = {
  displayName: string
}

type UpdateOwnPreferencesRequestContract = {
  preferences: UserPreferencesContract
}

type UpdateOwnAccountRequestContract = {
  email: string
  currentPassword: string
  newPassword: string | null
}

type UpdateManagedUserProfileRequestContract = {
  displayName: string
}

type UpdateManagedUserPreferencesRequestContract = {
  preferences: UserPreferencesContract
}

type UpdateManagedUserAccountRequestContract = {
  email: string
  newPassword: string | null
}

export function fromUserIdentityContract(response: UserIdentityContract): UserIdentity {
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
    query: request.query ? userSearchQueryValue(request.query) : null,
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
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
      autoMarkMessageRead: request.preferences.autoMarkMessageRead,
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
      autoMarkMessageRead: request.preferences.autoMarkMessageRead,
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
