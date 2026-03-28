type Brand<T, Name extends string> = T & { readonly __brand: Name }

export type Username = Brand<string, 'Username'>
export type DisplayName = Brand<string, 'DisplayName'>
export type EmailAddress = Brand<string, 'EmailAddress'>
export type PlaintextPassword = Brand<string, 'PlaintextPassword'>

export type AuthSession = {
  displayName: DisplayName
  username: Username
}

export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}

export type LoginResponse = AuthSession & {
  message: string
}

export type RegisterRequest = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
}

export type RegisterResponse = LoginResponse

export type AuthUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
}

export type ErrorResponse = {
  message: string
}

const authUserStorageKey = 'auth_user'
const usernamePattern = /^[A-Za-z0-9_-]+$/
const adminUsername = createUsername('admin')

export function createUsername(value: string): Username {
  return value as Username
}

export function createDisplayName(value: string): DisplayName {
  return value as DisplayName
}

export function createEmailAddress(value: string): EmailAddress {
  return value as EmailAddress
}

export function createPlaintextPassword(value: string): PlaintextPassword {
  return value as PlaintextPassword
}

export function usernameValue(username: Username): string {
  return username
}

export function displayNameValue(displayName: DisplayName): string {
  return displayName
}

export function emailAddressValue(emailAddress: EmailAddress): string {
  return emailAddress
}

export function plaintextPasswordValue(password: PlaintextPassword): string {
  return password
}

export function normalizeUsername(rawUsername: string): Username {
  return createUsername(rawUsername.trim())
}

export function normalizeDisplayName(rawDisplayName: string): DisplayName {
  return createDisplayName(rawDisplayName.trim())
}

export function normalizeEmailAddress(rawEmailAddress: string): EmailAddress {
  return createEmailAddress(rawEmailAddress.trim())
}

export function normalizePlaintextPassword(rawPassword: string): PlaintextPassword {
  return createPlaintextPassword(rawPassword.trim())
}

export function validateUsername(username: Username): string | null {
  const normalized = usernameValue(username)

  if (normalized.length < 3 || normalized.length > 32) {
    return 'Username must be between 3 and 32 characters.'
  }

  if (!usernamePattern.test(normalized)) {
    return 'Username may contain only letters, numbers, underscores, and hyphens.'
  }

  return null
}

export function toAuthSession(response: Pick<LoginResponse, 'displayName' | 'username'>): AuthSession {
  return {
    displayName: response.displayName,
    username: response.username,
  }
}

export function persistAuthSession(session: AuthSession): void {
  window.localStorage.setItem(authUserStorageKey, JSON.stringify(session))
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(authUserStorageKey)
}

export function readAuthSession(): AuthSession | null {
  const rawSession = window.localStorage.getItem(authUserStorageKey)
  if (!rawSession) {
    return null
  }

  try {
    const parsed = JSON.parse(rawSession) as { displayName?: unknown; username?: unknown }

    if (typeof parsed.displayName !== 'string' || typeof parsed.username !== 'string') {
      clearAuthSession()
      return null
    }

    return {
      displayName: createDisplayName(parsed.displayName),
      username: createUsername(parsed.username),
    }
  } catch {
    clearAuthSession()
    return null
  }
}

export function hasAuthSession(): boolean {
  return readAuthSession() !== null
}

export function isAdminUsername(username: Username): boolean {
  return usernameValue(username).toLowerCase() === usernameValue(adminUsername)
}

export function isAdminSession(session: AuthSession): boolean {
  return isAdminUsername(session.username)
}
