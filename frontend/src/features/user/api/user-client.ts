import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/domain/auth'
import type {
  AuthUserListItem,
  UpdateManagedUserSettingsRequest,
  UpdateOwnSettingsRequest,
  UpdateUserPermissionsRequest,
  UserAcceptedRanklistResponse,
  UserProfileResponse,
  UserRanklistResponse,
  Username,
} from '@/features/user/domain/user'
import {
  fromAuthUserListItemContract,
  fromUserAcceptedRanklistResponseContract,
  fromUserProfileResponseContract,
  fromUserRanklistResponseContract,
  toUpdateManagedUserSettingsRequestContract,
  toUpdateOwnSettingsRequestContract,
  toUpdateUserPermissionsRequestContract,
  usernameValue,
} from '@/features/user/domain/user'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'

export { HttpClientError as UserClientError } from '@/shared/api/http-client'

export async function listUsers(): Promise<AuthUserListItem[]> {
  return requestJson('/api/users', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid user list payload.')
    }

    return value.map(fromAuthUserListItemContract)
  })
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

export function updateOwnUserSettings(
  username: Username,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
    toUpdateOwnSettingsRequestContract(request),
  )
}

export function updateManagedUserSettings(
  username: Username,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
    toUpdateManagedUserSettingsRequestContract(request),
  )
}
