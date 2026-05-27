import type { AuthUserListItem } from '@/objects/user/response/AuthUserListItem'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'
import type { UserListRequest } from '@/objects/user/request/UserListRequest'
import type { UserListResponse } from '@/objects/user/response/UserListResponse'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import type { UserRanklistItem } from '@/objects/user/response/UserRanklistItem'
import { fromEmailAddressContract } from '@/objects/auth/EmailAddress'
import { fromDisplayNameContract, toDisplayNameContract } from '@/objects/user/DisplayName'
import { fromUserAcceptedProblemContract } from '@/objects/user/UserAcceptedProblem'
import { fromUserContributionContract } from '@/objects/user/UserContribution'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import { fromUserPreferencesContract, toUserPreferencesContract } from '@/objects/user/UserPreferences'
import { userSearchQueryValue } from '@/objects/user/request/UserSearchQuery'
import { fromUsernameContract } from '@/objects/user/Username'

export { fromUserIdentityContract } from '@/objects/user/UserIdentity'

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

type UserAcceptedProblemContract = {
  slug: string
  title: string
  acceptedAt: string
}

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
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

type SessionResponseContract = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferencesContract
  siteManager: boolean
  problemManager: boolean
}

type UpdateOwnProfileRequestContract = {
  displayName: string
}

type UpdateOwnPreferencesRequestContract = {
  preferences: UserPreferencesContract
}

type UpdateManagedUserProfileRequestContract = {
  displayName: string
}

type UpdateManagedUserPreferencesRequestContract = {
  preferences: UserPreferencesContract
}

export function fromUserProfileResponseContract(response: UserProfileResponseContract): UserProfileResponse {
  const acceptedProblems = Array.isArray(response.acceptedProblems) ? response.acceptedProblems : []

  return {
    username: fromUsernameContract(response.username, 'user profile username'),
    displayName: fromDisplayNameContract(response.displayName, 'user profile display name'),
    contribution: fromUserContributionContract(response.contribution, 'user profile contribution'),
    acceptedProblems: acceptedProblems.map(fromUserAcceptedProblemContract),
  }
}

export function fromUserRanklistItemContract(response: UserRanklistItemContract): UserRanklistItem {
  return {
    user: fromUserIdentityContract(response.user),
    contribution: fromUserContributionContract(response.contribution, 'user ranklist contribution'),
  }
}

export function fromUserRanklistResponseContract(
  response: PageResponseContract<UserRanklistItemContract>,
): PageResponse<UserRanklistItem> {
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
): PageResponse<UserAcceptedRanklistItem> {
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
    username: fromUsernameContract(response.username, 'auth user username'),
    displayName: fromDisplayNameContract(response.displayName, 'auth user display name'),
    email: fromEmailAddressContract(response.email, 'auth user email'),
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

export function fromSessionResponseContract(response: SessionResponseContract): SessionResponse {
  return {
    displayName: fromDisplayNameContract(response.displayName, 'session response display name'),
    username: fromUsernameContract(response.username, 'session response username'),
    email: fromEmailAddressContract(response.email, 'session response email'),
    preferences: fromUserPreferencesContract(response.preferences, 'session response'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function toUserListRequestContract(request: UserListRequest): UserListRequestContract {
  return {
    query: request.query ? userSearchQueryValue(request.query) : null,
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
  }
}

export function toUpdateOwnProfileRequestContract(
  request: UpdateOwnProfileRequest,
): UpdateOwnProfileRequestContract {
  return {
    displayName: toDisplayNameContract(request.displayName),
  }
}

export function toUpdateOwnPreferencesRequestContract(
  request: UpdateOwnPreferencesRequest,
): UpdateOwnPreferencesRequestContract {
  return {
    preferences: toUserPreferencesContract(request.preferences),
  }
}

export function toUpdateManagedUserProfileRequestContract(
  request: UpdateManagedUserProfileRequest,
): UpdateManagedUserProfileRequestContract {
  return {
    displayName: toDisplayNameContract(request.displayName),
  }
}

export function toUpdateManagedUserPreferencesRequestContract(
  request: UpdateManagedUserPreferencesRequest,
): UpdateManagedUserPreferencesRequestContract {
  return {
    preferences: toUserPreferencesContract(request.preferences),
  }
}
