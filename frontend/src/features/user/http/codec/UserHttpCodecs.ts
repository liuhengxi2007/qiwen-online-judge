import type { AuthUserListItem } from '@/features/user/model/response/AuthUserListItem'
import type { PageResponse } from '@/shared/model/PageResponse'
import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/features/user/model/request/UpdateManagedUserAccountRequest'
import type { UpdateManagedUserPreferencesRequest } from '@/features/user/model/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/features/user/model/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnAccountRequest } from '@/features/user/model/request/UpdateOwnAccountRequest'
import type { UpdateOwnPreferencesRequest } from '@/features/user/model/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/features/user/model/request/UpdateOwnProfileRequest'
import type { UpdateUserPermissionsRequest } from '@/features/user/model/request/UpdateUserPermissionsRequest'
import type { UserAcceptedRanklistItem } from '@/features/user/model/response/UserAcceptedRanklistItem'
import type { UserListRequest } from '@/features/user/model/request/UserListRequest'
import type { UserListResponse } from '@/features/user/model/response/UserListResponse'
import type { UserProfileResponse } from '@/features/user/model/response/UserProfileResponse'
import type { UserRanklistItem } from '@/features/user/model/response/UserRanklistItem'
import {
  fromEmailAddressContract,
  toEmailAddressContract,
  toPlaintextPasswordContract,
} from '@/features/auth/http/codec/AuthModelHttpCodecs'
import {
  fromDisplayNameContract,
  fromUserAcceptedProblemContract,
  fromUserContributionContract,
  fromUserIdentityContract,
  fromUserPreferencesContract,
  fromUsernameContract,
  toDisplayNameContract,
  toUserPreferencesContract,
  type UserAcceptedProblemContract,
  type UserIdentityContract,
  type UserPreferencesContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'
import { userSearchQueryValue } from '@/features/user/lib/user-parsers'

export { fromUserIdentityContract } from '@/features/user/http/codec/UserModelHttpCodecs'

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

export function toUpdateUserPermissionsRequestContract(
  request: UpdateUserPermissionsRequest,
): UpdateUserPermissionsRequestContract {
  return request
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

export function toUpdateOwnAccountRequestContract(
  request: UpdateOwnAccountRequest,
): UpdateOwnAccountRequestContract {
  return {
    email: toEmailAddressContract(request.email),
    currentPassword: toPlaintextPasswordContract(request.currentPassword),
    newPassword: request.newPassword ? toPlaintextPasswordContract(request.newPassword) : null,
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

export function toUpdateManagedUserAccountRequestContract(
  request: UpdateManagedUserAccountRequest,
): UpdateManagedUserAccountRequestContract {
  return {
    email: toEmailAddressContract(request.email),
    newPassword: request.newPassword ? toPlaintextPasswordContract(request.newPassword) : null,
  }
}
