import type {
  DisplayName,
  EmailAddress,
  PlaintextPassword,
  Username,
} from '@/features/auth/model/AuthValues'
import type { UserDisplayMode } from '@/features/auth/model/UserDisplayMode'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
export type ParseResult<T> = ParseSuccess<T> | ParseFailure

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

function createUserDisplayMode(value: UserDisplayMode): UserDisplayMode {
  return value
}

export function requireParsed<T>(result: ParseResult<T>, label: string): T {
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

export function userDisplayModeValue(displayMode: UserDisplayMode): UserDisplayMode {
  return displayMode
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

export function parseUserDisplayMode(rawDisplayMode: string): ParseResult<UserDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'display_name':
    case 'username':
    case 'display_name_with_username':
      return { ok: true, value: createUserDisplayMode(normalized) }
    default:
      return {
        ok: false,
        error: 'Display mode must be one of: display_name, username, display_name_with_username.',
      }
  }
}
