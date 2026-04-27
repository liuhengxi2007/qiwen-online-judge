import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/domain/auth'
import type {
  AuthUserListItem,
  UserListRequest,
  UserListResponse,
  UpdateManagedUserAccountRequest,
  UpdateManagedUserPreferencesRequest,
  UpdateManagedUserProfileRequest,
  UpdateOwnAccountRequest,
  UpdateOwnPreferencesRequest,
  UpdateOwnProfileRequest,
  UpdateUserPermissionsRequest,
  UserAcceptedRanklistResponse,
  UserIdentity,
  UserProfileResponse,
  UserRanklistResponse,
  Username,
} from '@/features/user/domain/user'
import {
  fromAuthUserListItemContract,
  fromUserListResponseContract,
  fromUserAcceptedRanklistResponseContract,
  fromUserIdentityContract,
  fromUserProfileResponseContract,
  fromUserRanklistResponseContract,
  parseUserSearchQuery,
  toUserListRequestContract,
  toUpdateManagedUserAccountRequestContract,
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnAccountRequestContract,
  toUpdateOwnPreferencesRequestContract,
  toUpdateOwnProfileRequestContract,
  toUpdateUserPermissionsRequestContract,
  usernameValue,
} from '@/features/user/domain/user'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'

export { HttpClientError as UserClientError } from '@/shared/api/http-client'

export async function listUsers(request: UserListRequest): Promise<UserListResponse> {
  const url = new URL('/api/users', window.location.origin)
  const contractRequest = toUserListRequestContract(request)
  if (contractRequest.query !== null && contractRequest.query.trim()) {
    url.searchParams.set('q', contractRequest.query)
  }
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))
  return requestJson(url.pathname + url.search, fromUserListResponseContract)
}

export function updateUserPermissions(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/permissions`,
    fromAuthUserListItemContract,
    toUpdateUserPermissionsRequestContract(request),
  )
}

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
  )
}

export async function getUserProfile(username: Username): Promise<UserProfileResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/profile`,
    fromUserProfileResponseContract,
  )
}

export async function listUserSuggestions(query: string): Promise<UserIdentity[]> {
  const parsedQuery = parseUserSearchQuery(query)
  if (!parsedQuery.ok) {
    return []
  }

  const url = new URL('/api/users/suggestions', window.location.origin)
  url.searchParams.set('q', parsedQuery.value)
  return requestJson(url.pathname + url.search, (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid user suggestion payload.')
    }

    return value.map(fromUserIdentityContract)
  })
}

export async function listContributionRanklist(page: number): Promise<UserRanklistResponse> {
  return requestJson(`/api/users/ranklist?page=${encodeURIComponent(String(page))}`, fromUserRanklistResponseContract)
}

export async function listAcceptedRanklist(page: number): Promise<UserAcceptedRanklistResponse> {
  return requestJson(
    `/api/users/ranklist/accepted?page=${encodeURIComponent(String(page))}`,
    fromUserAcceptedRanklistResponseContract,
  )
}

export function deleteUser(username: Username): Promise<SuccessResponse> {
  return postJson(`/api/users/${encodeURIComponent(usernameValue(username))}/delete`, decodeSuccessResponse, {})
}

export function updateOwnUserProfile(
  username: Username,
  request: UpdateOwnProfileRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/profile`,
    fromSessionResponseContract,
    toUpdateOwnProfileRequestContract(request),
  )
}

export function updateOwnUserPreferences(
  username: Username,
  request: UpdateOwnPreferencesRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/preferences`,
    fromSessionResponseContract,
    toUpdateOwnPreferencesRequestContract(request),
  )
}

export function updateOwnUserAccount(
  username: Username,
  request: UpdateOwnAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateOwnAccountRequestContract(request),
  )
}

export function updateManagedUserProfile(
  username: Username,
  request: UpdateManagedUserProfileRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/profile`,
    fromSessionResponseContract,
    toUpdateManagedUserProfileRequestContract(request),
  )
}

export function updateManagedUserPreferences(
  username: Username,
  request: UpdateManagedUserPreferencesRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/preferences`,
    fromSessionResponseContract,
    toUpdateManagedUserPreferencesRequestContract(request),
  )
}

export function updateManagedUserAccount(
  username: Username,
  request: UpdateManagedUserAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateManagedUserAccountRequestContract(request),
  )
}
