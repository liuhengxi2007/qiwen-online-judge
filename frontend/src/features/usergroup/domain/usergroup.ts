import type {
  AddUserGroupMemberRequest as AddUserGroupMemberRequestContract,
  CreateUserGroupRequest as CreateUserGroupRequestContract,
  UpdateUserGroupMemberRoleRequest as UpdateUserGroupMemberRoleRequestContract,
  UpdateUserGroupRequest as UpdateUserGroupRequestContract,
  UserGroupDetail as UserGroupDetailContract,
  UserGroupListResponse as UserGroupListResponseContract,
  UserGroupMember as UserGroupMemberContract,
  UserGroupSummary as UserGroupSummaryContract,
} from '@contracts/usergroup'
import { parseDisplayName, parseUsername } from '@/features/auth/domain/auth'
import type {
  AddUserGroupMemberRequest
} from '@/features/usergroup/model/AddUserGroupMemberRequest'
import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'
import type { CreateUserGroupRequest } from '@/features/usergroup/model/CreateUserGroupRequest'
import type { UpdateUserGroupMemberRoleRequest } from '@/features/usergroup/model/UpdateUserGroupMemberRoleRequest'
import type { UpdateUserGroupRequest } from '@/features/usergroup/model/UpdateUserGroupRequest'
import type { UserGroupDescription } from '@/features/usergroup/model/UserGroupDescription'
import type { UserGroupDetail } from '@/features/usergroup/model/UserGroupDetail'
import type { UserGroupId } from '@/features/usergroup/model/UserGroupId'
import type { UserGroupListResponse } from '@/features/usergroup/model/UserGroupListResponse'
import type { UserGroupMember } from '@/features/usergroup/model/UserGroupMember'
import type { UserGroupName } from '@/features/usergroup/model/UserGroupName'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import type { UserGroupSummary } from '@/features/usergroup/model/UserGroupSummary'

type ParseSuccess<T> = { ok: true; value: T }
type ParseFailure = { ok: false; error: string }
type ParseResult<T> = ParseSuccess<T> | ParseFailure

export type {
  AddUserGroupMemberRequest,
  AddUserGroupMemberRole,
  CreateUserGroupRequest,
  UpdateUserGroupMemberRoleRequest,
  UpdateUserGroupRequest,
  UserGroupDescription,
  UserGroupDetail,
  UserGroupId,
  UserGroupListResponse,
  UserGroupMember,
  UserGroupName,
  UserGroupRole,
  UserGroupSlug,
  UserGroupSummary,
}

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

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

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
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

export function fromUserGroupMemberContract(member: UserGroupMemberContract): UserGroupMember {
  return {
    username: requireParsed(parseUsername(member.username), 'user group member username'),
    displayName: requireParsed(parseDisplayName(member.displayName), 'user group member display name'),
    role: requireParsed(parseUserGroupRole(member.role), 'user group member role'),
    joinedAt: member.joinedAt,
  }
}

export function fromUserGroupSummaryContract(group: UserGroupSummaryContract): UserGroupSummary {
  return {
    id: requireParsed(parseUserGroupId(group.id), 'user group summary id'),
    slug: requireParsed(parseUserGroupSlug(group.slug), 'user group summary slug'),
    name: requireParsed(parseUserGroupName(group.name), 'user group summary name'),
    description: requireParsed(parseUserGroupDescription(group.description), 'user group summary description'),
    ownerUsername: requireParsed(parseUsername(group.ownerUsername), 'user group summary owner username'),
    createdAt: group.createdAt,
    updatedAt: group.updatedAt,
  }
}

export function fromUserGroupDetailContract(group: UserGroupDetailContract): UserGroupDetail {
  return {
    id: requireParsed(parseUserGroupId(group.id), 'user group detail id'),
    slug: requireParsed(parseUserGroupSlug(group.slug), 'user group detail slug'),
    name: requireParsed(parseUserGroupName(group.name), 'user group detail name'),
    description: requireParsed(parseUserGroupDescription(group.description), 'user group detail description'),
    ownerUsername: requireParsed(parseUsername(group.ownerUsername), 'user group detail owner username'),
    members: group.members.map(fromUserGroupMemberContract),
    createdAt: group.createdAt,
    updatedAt: group.updatedAt,
  }
}

export function fromUserGroupListResponseContract(response: UserGroupListResponseContract): UserGroupListResponse {
  return {
    items: response.items.map(fromUserGroupSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateUserGroupRequestContract(request: CreateUserGroupRequest): CreateUserGroupRequestContract {
  return {
    slug: userGroupSlugValue(request.slug),
    name: userGroupNameValue(request.name),
    description: userGroupDescriptionValue(request.description),
  }
}

export function toUpdateUserGroupRequestContract(request: UpdateUserGroupRequest): UpdateUserGroupRequestContract {
  return {
    name: userGroupNameValue(request.name),
    description: userGroupDescriptionValue(request.description),
  }
}

export function toAddUserGroupMemberRequestContract(
  request: AddUserGroupMemberRequest,
): AddUserGroupMemberRequestContract {
  return {
    username: request.username,
    role: request.role,
  }
}

export function toUpdateUserGroupMemberRoleRequestContract(
  request: UpdateUserGroupMemberRoleRequest,
): UpdateUserGroupMemberRoleRequestContract {
  return request
}
