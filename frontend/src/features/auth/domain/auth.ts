import type {
  AuthUserListItem as AuthUserListItemContract,
  LoginRequest as LoginRequestContract,
  LoginResponse as LoginResponseContract,
  RegisterRequest as RegisterRequestContract,
  RegisterResponse as RegisterResponseContract,
  SessionResponse as SessionResponseContract,
  UpdateManagedUserSettingsRequest as UpdateManagedUserSettingsRequestContract,
  UpdateOwnSettingsRequest as UpdateOwnSettingsRequestContract,
  UpdateUserPermissionsRequest as UpdateUserPermissionsRequestContract,
} from '@contracts/auth'
import type { ErrorResponse as ErrorResponseContract } from '@contracts/shared'

type Brand<T, Name extends string> = T & { readonly __brand: Name }
type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

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

export type ErrorResponse = ErrorResponseContract

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

export type UpdateManagedUserSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  newPassword: PlaintextPassword | null
}

const usernamePattern = /^[A-Za-z0-9_-]+$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function createUsername(value: string): Username {
  return value as Username
}

function createDisplayName(value: string): DisplayName {
  return value as DisplayName
}

function createEmailAddress(value: string): EmailAddress {
  return value as EmailAddress
}

function createPlaintextPassword(value: string): PlaintextPassword {
  return value as PlaintextPassword
}

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function usernameValue(username: Username): string {
  return username
}

export function sameUsername(left: Username, right: Username): boolean {
  return left.toLowerCase() === right.toLowerCase()
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

export function parseUsername(rawUsername: string): ParseResult<Username> {
  const normalized = rawUsername.trim()

  if (!normalized) {
    return { ok: false, error: 'Username is required.' }
  }

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: createUsername(normalized) }
}

export function parseDisplayName(rawDisplayName: string): ParseResult<DisplayName> {
  const normalized = rawDisplayName.trim()

  if (!normalized) {
    return { ok: false, error: 'Display name is required.' }
  }

  return { ok: true, value: createDisplayName(normalized) }
}

export function parseEmailAddress(rawEmailAddress: string): ParseResult<EmailAddress> {
  const normalized = rawEmailAddress.trim()

  if (!normalized) {
    return { ok: false, error: 'Email is required.' }
  }

  if (!emailPattern.test(normalized)) {
    return { ok: false, error: 'Please enter a valid email address.' }
  }

  return { ok: true, value: createEmailAddress(normalized) }
}

export function parsePlaintextPassword(rawPassword: string): ParseResult<PlaintextPassword> {
  const normalized = rawPassword.trim()

  if (!normalized) {
    return { ok: false, error: 'Password is required.' }
  }

  return { ok: true, value: createPlaintextPassword(normalized) }
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

export function asSiteManagerSession(session: AuthSession): SiteManagerSession | null {
  return session.siteManager ? (session as SiteManagerSession) : null
}

export function asProblemManagerSession(session: AuthSession): ProblemManagerSession | null {
  return session.problemManager ? (session as ProblemManagerSession) : null
}

export function toLoginRequestContract(request: LoginRequest): LoginRequestContract {
  return {
    username: usernameValue(request.username),
    password: plaintextPasswordValue(request.password),
  }
}

export function fromLoginResponseContract(response: LoginResponseContract): LoginResponse {
  return {
    displayName: requireParsed(parseDisplayName(response.displayName), 'login response display name'),
    username: requireParsed(parseUsername(response.username), 'login response username'),
    email: requireParsed(parseEmailAddress(response.email), 'login response email'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
    message: response.message,
  }
}

export function toRegisterRequestContract(request: RegisterRequest): RegisterRequestContract {
  return {
    username: usernameValue(request.username),
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    password: plaintextPasswordValue(request.password),
  }
}

export function fromRegisterResponseContract(response: RegisterResponseContract): RegisterResponse {
  return fromLoginResponseContract(response)
}

export function fromSessionResponseContract(response: SessionResponseContract): SessionResponse {
  return {
    displayName: requireParsed(parseDisplayName(response.displayName), 'session response display name'),
    username: requireParsed(parseUsername(response.username), 'session response username'),
    email: requireParsed(parseEmailAddress(response.email), 'session response email'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function fromAuthUserListItemContract(response: AuthUserListItemContract): AuthUserListItem {
  return {
    username: requireParsed(parseUsername(response.username), 'auth user username'),
    displayName: requireParsed(parseDisplayName(response.displayName), 'auth user display name'),
    email: requireParsed(parseEmailAddress(response.email), 'auth user email'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function toUpdateUserPermissionsRequestContract(
  request: UpdateUserPermissionsRequest,
): UpdateUserPermissionsRequestContract {
  return request
}

export function toUpdateOwnSettingsRequestContract(
  request: UpdateOwnSettingsRequest,
): UpdateOwnSettingsRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    currentPassword: plaintextPasswordValue(request.currentPassword),
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}

export function toUpdateManagedUserSettingsRequestContract(
  request: UpdateManagedUserSettingsRequest,
): UpdateManagedUserSettingsRequestContract {
  return {
    displayName: displayNameValue(request.displayName),
    email: emailAddressValue(request.email),
    newPassword: request.newPassword ? plaintextPasswordValue(request.newPassword) : null,
  }
}
