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
} from '@/domain/auth'

type ErrorResponse = {
  message?: string
}

export class AuthClientError extends Error {
  readonly kind: 'unauthorized' | 'forbidden' | 'not-found' | 'http'

  constructor(kind: 'unauthorized' | 'forbidden' | 'not-found' | 'http', message: string) {
    super(message)
    this.kind = kind
  }
}

async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const errorData = (await response.json().catch(() => null)) as ErrorResponse | null
  return errorData?.message ?? fallback
}

async function requestJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: 'same-origin',
    ...init,
  })

  if (response.status === 401) {
    throw new AuthClientError('unauthorized', await readErrorMessage(response, 'Authentication required.'))
  }

  if (response.status === 403) {
    throw new AuthClientError('forbidden', await readErrorMessage(response, 'Forbidden.'))
  }

  if (response.status === 404) {
    throw new AuthClientError('not-found', await readErrorMessage(response, 'Not found.'))
  }

  if (!response.ok) {
    throw new AuthClientError('http', await readErrorMessage(response, 'Request failed.'))
  }

  return (await response.json()) as T
}

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
  return requestJson<LoginResponse>('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return requestJson<RegisterResponse>('/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

export function listUsers(): Promise<AuthUserListItem[]> {
  return requestJson<AuthUserListItem[]>('/api/auth/users')
}

export function updateUserPermissions(
  username: string,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return requestJson<AuthUserListItem>(`/api/auth/users/${encodeURIComponent(username)}/permissions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

export function getUserSettings(username: string): Promise<SessionResponse> {
  return requestJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`)
}

export function updateOwnUserSettings(
  username: string,
  request: UpdateOwnSettingsRequest,
): Promise<SessionResponse> {
  return requestJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

export function updateManagedUserSettings(
  username: string,
  request: UpdateManagedUserSettingsRequest,
): Promise<SessionResponse> {
  return requestJson<SessionResponse>(`/api/auth/users/${encodeURIComponent(username)}/settings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}
