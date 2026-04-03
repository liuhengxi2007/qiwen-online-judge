import type {
  AuthUserListItem as AuthUserListItemContract,
  LoginResponse as LoginResponseContract,
  RegisterResponse as RegisterResponseContract,
  SessionResponse as SessionResponseContract,
} from '@contracts/auth'
import type {
  AuthUserListItem,
  LoginRequest,
  LoginResponse,
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
  fromRegisterResponseContract,
  fromSessionResponseContract,
  toLoginRequestContract,
  toRegisterRequestContract,
  toUpdateManagedUserSettingsRequestContract,
  toUpdateOwnSettingsRequestContract,
  toUpdateUserPermissionsRequestContract,
  usernameValue,
} from '@/features/auth/domain/auth'
import { postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'

export { HttpClientError as AuthClientError } from '@/shared/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  const response = await requestJson<SessionResponseContract>('/api/auth/session')
  return fromSessionResponseContract(response)
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  }).catch(() => undefined)
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await postJson<LoginResponseContract>('/api/auth/login', toLoginRequestContract(request))
  return fromLoginResponseContract(response)
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  const response = await postJson<RegisterResponseContract>('/api/auth/register', toRegisterRequestContract(request))
  return fromRegisterResponseContract(response)
}

export async function listUsers(): Promise<AuthUserListItem[]> {
  const response = await requestJson<AuthUserListItemContract[]>('/api/auth/users')
  return response.map(fromAuthUserListItemContract)
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
  const response = await postJson<AuthUserListItemContract>(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/permissions`,
    toUpdateUserPermissionsRequestContract(request),
  )
  return fromAuthUserListItemContract(response)
}

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  const response = await requestJson<SessionResponseContract>(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
  )
  return fromSessionResponseContract(response)
}

export function deleteUser(username: Username): Promise<SuccessResponse> {
  return postJson<SuccessResponse>(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/delete`, {})
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
  const response = await postJson<SessionResponseContract>(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
    toUpdateOwnSettingsRequestContract(request),
  )
  return fromSessionResponseContract(response)
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
  const response = await postJson<SessionResponseContract>(
    `/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`,
    toUpdateManagedUserSettingsRequestContract(request),
  )
  return fromSessionResponseContract(response)
}
