type Brand<T, Name extends string> = T & { readonly __brand: Name }

export type Username = Brand<string, 'Username'>
export type DisplayName = Brand<string, 'DisplayName'>
export type EmailAddress = Brand<string, 'EmailAddress'>
export type PlaintextPassword = Brand<string, 'PlaintextPassword'>

export type AuthSession = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}

export type SiteManagerSession = AuthSession & {
  siteManager: true
}

export type ProblemManagerSession = AuthSession & {
  problemManager: true
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
export type SessionResponse = AuthSession

export type AuthUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}

export type ErrorResponse = {
  message: string
}

export type UpdateUserPermissionsRequest = {
  siteManager: boolean
  problemManager: boolean
}

export type UpdateOwnSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}

const authUserStorageKey = 'auth_user'
const usernamePattern = /^[A-Za-z0-9_-]+$/
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

export function toAuthSession(
  response: Pick<LoginResponse, 'displayName' | 'username' | 'email' | 'siteManager' | 'problemManager'>,
): AuthSession {
  return {
    displayName: response.displayName,
    username: response.username,
    email: response.email,
    siteManager: response.siteManager,
    problemManager: response.problemManager,
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
    const parsed = JSON.parse(rawSession) as {
      displayName?: unknown
      username?: unknown
      email?: unknown
      siteManager?: unknown
      problemManager?: unknown
    }

    if (
      typeof parsed.displayName !== 'string' ||
      typeof parsed.username !== 'string' ||
      typeof parsed.email !== 'string' ||
      typeof parsed.siteManager !== 'boolean' ||
      typeof parsed.problemManager !== 'boolean'
    ) {
      clearAuthSession()
      return null
    }

    return {
      displayName: createDisplayName(parsed.displayName),
      username: createUsername(parsed.username),
      email: createEmailAddress(parsed.email),
      siteManager: parsed.siteManager,
      problemManager: parsed.problemManager,
    }
  } catch {
    clearAuthSession()
    return null
  }
}

export function hasAuthSession(): boolean {
  return readAuthSession() !== null
}

export function asSiteManagerSession(session: AuthSession): SiteManagerSession | null {
  return session.siteManager ? (session as SiteManagerSession) : null
}

export function asProblemManagerSession(session: AuthSession): ProblemManagerSession | null {
  return session.problemManager ? (session as ProblemManagerSession) : null
}
