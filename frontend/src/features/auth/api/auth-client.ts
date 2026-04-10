import type {
  AuthUserListItem,
  LoginRequest,
  LoginResponse,
  RegisteredJudgerListItem,
  RegisterRequest,
  RegisterResponse,
  SessionResponse,
  UpdateManagedUserSettingsRequest,
  UpdateOwnSettingsRequest,
  UpdateUserPermissionsRequest,
  Username,
} from '@/features/auth/domain/auth'
import {
  fromAuthUserListItemContract,
  fromLoginResponseContract,
  fromRegisteredJudgerListItemContract,
  fromRegisterResponseContract,
  fromSessionResponseContract,
  toLoginRequestContract,
  toRegisterRequestContract,
  toUpdateManagedUserSettingsRequestContract,
  toUpdateOwnSettingsRequestContract,
  toUpdateUserPermissionsRequestContract,
  usernameValue,
} from '@/features/auth/domain/auth'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'

export { HttpClientError as AuthClientError } from '@/shared/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  return requestJson('/api/auth/session', fromSessionResponseContract)
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  }).catch(() => undefined)
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson('/api/auth/login', fromLoginResponseContract, toLoginRequestContract(request))
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson('/api/auth/register', fromRegisterResponseContract, toRegisterRequestContract(request))
}

export async function listUsers(): Promise<AuthUserListItem[]> {
  return requestJson('/api/auth/users', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid auth user list payload.')
    }

    return value.map(fromAuthUserListItemContract)
  })
}

export async function listRegisteredJudgers(): Promise<RegisteredJudgerListItem[]> {
  return requestJson('/api/auth/judgers', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid registered judger list payload.')
    }

    return value.map(fromRegisteredJudgerListItemContract)
  })
}

export function updateUserPermissions(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return updateUserPermissionsInternal(username, request)
}

async function updateUserPermissionsInternal(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return postJson(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/permissions`,
    fromAuthUserListItemContract,
    toUpdateUserPermissionsRequestContract(request),
  )
}

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
  )
}

export function deleteUser(username: Username): Promise<SuccessResponse> {
  return postJson(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/delete`, decodeSuccessResponse, {})
}

export function updateOwnUserSettings(
  username: Username,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return updateOwnUserSettingsInternal(username, request)
}

async function updateOwnUserSettingsInternal(
  username: Username,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
    toUpdateOwnSettingsRequestContract(request),
  )
}

export function updateManagedUserSettings(
  username: Username,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return updateManagedUserSettingsInternal(username, request)
}

async function updateManagedUserSettingsInternal(
  username: Username,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
    toUpdateManagedUserSettingsRequestContract(request),
  )
}
