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
} from '@/features/auth/domain/auth'
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
  username: string,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return postJson<AuthUserListItem>(`/api/auth/users/${encodeURIComponent(username)}/permissions`, request)
}

export function getUserSettings(username: string): Promise<SessionResponse> {
  return requestJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`)
}

export function updateOwnUserSettings(
  username: string,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return postJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`, request)
}

export function updateManagedUserSettings(
  username: string,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return postJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`, request)
}
