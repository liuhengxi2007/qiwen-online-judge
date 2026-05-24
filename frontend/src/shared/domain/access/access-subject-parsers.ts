import type { AccessUserGroupSlug } from '@/shared/model/access/AccessUserGroupSlug'
import type { AccessUsername } from '@/shared/model/access/AccessUsername'
import type { ParseResult } from '@/shared/domain/parsing'

const usernamePattern = /^[a-z0-9_-]+$/
const userGroupSlugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

export const resourceAccessSubjectParsers = {
  parseUsername,
  parseUserGroupSlug,
}

export function accessUsernameValue(username: AccessUsername): string {
  return username
}

export function accessUserGroupSlugValue(slug: AccessUserGroupSlug): string {
  return slug
}

export function parseUsername(rawUsername: string): ParseResult<AccessUsername> {
  const normalized = rawUsername.trim().toLowerCase()

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: normalized as AccessUsername }
}

export function parseUserGroupSlug(rawSlug: string): ParseResult<AccessUserGroupSlug> {
  const normalized = rawSlug.trim()

  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }

  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }

  if (!userGroupSlugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  return { ok: true, value: normalized as AccessUserGroupSlug }
}
