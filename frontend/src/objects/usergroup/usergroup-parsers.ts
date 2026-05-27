import type { AddUserGroupMemberRole } from '@/objects/usergroup/AddUserGroupMemberRole'
import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupId } from '@/objects/usergroup/UserGroupId'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }

export type ParseResult<T> = ParseSuccess<T> | ParseFailure

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createUserGroupId(value: string): UserGroupId {
  return value as UserGroupId
}

function createUserGroupSlug(value: string): UserGroupSlug {
  return value as UserGroupSlug
}

function createUserGroupName(value: string): UserGroupName {
  return value as UserGroupName
}

function createUserGroupDescription(value: string): UserGroupDescription {
  return value as UserGroupDescription
}

export function userGroupSlugValue(slug: UserGroupSlug): string {
  return slug
}

export function userGroupNameValue(name: UserGroupName): string {
  return name
}

export function userGroupDescriptionValue(description: UserGroupDescription): string {
  return description
}

export function parseUserGroupId(rawId: string): ParseResult<UserGroupId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'User group id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'User group id must be a valid UUID.' }
  }

  return { ok: true, value: createUserGroupId(normalized) }
}

export function parseUserGroupSlug(rawSlug: string): ParseResult<UserGroupSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  return { ok: true, value: createUserGroupSlug(normalized) }
}

export function parseUserGroupName(rawName: string): ParseResult<UserGroupName> {
  const normalized = rawName.trim()
  if (!normalized) {
    return { ok: false, error: 'User group name is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'User group name must be at most 120 characters.' }
  }

  return { ok: true, value: createUserGroupName(normalized) }
}

export function parseUserGroupDescription(rawDescription: string): ParseResult<UserGroupDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 2000) {
    return { ok: false, error: 'User group description must be at most 2000 characters.' }
  }

  return { ok: true, value: createUserGroupDescription(normalized) }
}

export function parseUserGroupRole(rawRole: string): ParseResult<UserGroupRole> {
  if (rawRole === 'owner' || rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'Unknown user group role.' }
}

export function parseAddUserGroupMemberRole(rawRole: string): ParseResult<AddUserGroupMemberRole> {
  if (rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'New members may only be added as member or manager.' }
}
