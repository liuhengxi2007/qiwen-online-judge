import type {
  AuthUserListItem as AuthUserListItemContract,
  LoginRequest as LoginRequestContract,
  LoginResponse as LoginResponseContract,
  RegisteredJudgerListItem as RegisteredJudgerListItemContract,
  RegisterRequest as RegisterRequestContract,
  RegisterResponse as RegisterResponseContract,
  SessionResponse as SessionResponseContract,
  UpdateManagedUserSettingsRequest as UpdateManagedUserSettingsRequestContract,
  UpdateOwnSettingsRequest as UpdateOwnSettingsRequestContract,
  UpdateUserPermissionsRequest as UpdateUserPermissionsRequestContract,
} from '@contracts/auth'
import type { ErrorResponse as ErrorResponseContract } from '@contracts/shared'
import type { AuthUserListItem } from '@/features/auth/model/AuthUserListItem'
import type {
  DisplayName,
  EmailAddress,
  PlaintextPassword,
  Username,
} from '@/features/auth/model/AuthValues'
import type { LoginRequest } from '@/features/auth/model/LoginRequest'
import type { LoginResponse } from '@/features/auth/model/LoginResponse'
import type { RegisterRequest } from '@/features/auth/model/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/model/RegisterResponse'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'
import type { UpdateManagedUserSettingsRequest } from '@/features/auth/model/UpdateManagedUserSettingsRequest'
import type { UpdateOwnSettingsRequest } from '@/features/auth/model/UpdateOwnSettingsRequest'
import type { UpdateUserPermissionsRequest } from '@/features/auth/model/UpdateUserPermissionsRequest'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

export type ErrorResponse = ErrorResponseContract

export type {
  AuthUserListItem,
  DisplayName,
  EmailAddress,
  LoginRequest,
  LoginResponse,
  PlaintextPassword,
  RegisterRequest,
  RegisterResponse,
  RegisteredJudgerListItem,
  SessionResponse,
  UpdateManagedUserSettingsRequest,
  UpdateOwnSettingsRequest,
  UpdateUserPermissionsRequest,
  Username,
}

const usernamePattern = /^[a-z0-9_-]+$/
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
  const normalized = rawUsername.trim().toLowerCase()

  if (!normalized) {
    return { ok: false, error: 'Username is required.' }
  }

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: createUsername(normalized) }
}

export function parseDisplayName(rawDisplayName: string): ParseResult<DisplayName> {
  const normalized = rawDisplayName.trim()

  if (!normalized) {
    return { ok: false, error: 'Display name is required.' }
  }

  if (normalized.length > 120) {
    return { ok: false, error: 'Display name must be at most 120 characters.' }
  }

  return { ok: true, value: createDisplayName(normalized) }
}

export function parseEmailAddress(rawEmailAddress: string): ParseResult<EmailAddress> {
  const normalized = rawEmailAddress.trim()

  if (!normalized) {
    return { ok: false, error: 'Email is required.' }
  }

  if (normalized.length > 255) {
    return { ok: false, error: 'Email must be at most 255 characters.' }
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
): SessionResponse {
  return {
    displayName: response.displayName,
    username: response.username,
    email: response.email,
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function asSiteManagerSession(session: SessionResponse): (SessionResponse & { siteManager: true }) | null {
  return isSiteManagerSession(session) ? session : null
}

export function asProblemManagerSession(
  session: SessionResponse,
): (SessionResponse & { problemManager: true }) | null {
  return isProblemManagerSession(session) ? session : null
}

export function isSiteManagerSession(session: SessionResponse): session is SessionResponse & { siteManager: true } {
  return session.siteManager
}

export function isProblemManagerSession(
  session: SessionResponse,
): session is SessionResponse & { problemManager: true } {
  return session.problemManager
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

export function fromRegisteredJudgerListItemContract(response: RegisteredJudgerListItemContract): RegisteredJudgerListItem {
  return {
    judgerId: response.judgerId,
    requestedPrefix: response.requestedPrefix,
    host: response.host.trim(),
    processId: response.processId?.trim() || null,
    supportedLanguages: response.supportedLanguages.map((language) => language.trim()).filter((language) => language.length > 0),
    registeredAt: response.registeredAt,
    lastHeartbeatAt: response.lastHeartbeatAt,
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
