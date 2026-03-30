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
import { usernameValue } from '@/features/auth/domain/auth'
import { postJson, requestJson } from '@/shared/api/http-client'

export { HttpClientError as AuthClientError } from '@/shared/api/http-client'

export function getSession(): Promise<SessionResponse> {
  return requestJson<SessionResponse>('/api/auth/session')
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  }).catch(() => undefined)
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson<LoginResponse>('/api/auth/login', request)
}

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson<RegisterResponse>('/api/auth/register', request)
}

export function listUsers(): Promise<AuthUserListItem[]> {
  return requestJson<AuthUserListItem[]>('/api/auth/users')
}

export function updateUserPermissions(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return postJson<AuthUserListItem>(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/permissions`, request)
}

export function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`)
}

export function updateOwnUserSettings(
  username: Username,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return postJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`, request)
}

export function updateManagedUserSettings(
  username: Username,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return postJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(usernameValue(username))}/settings`, request)
}
