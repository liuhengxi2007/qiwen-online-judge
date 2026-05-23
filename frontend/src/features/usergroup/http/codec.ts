import { parseDisplayName, parseUsername } from '@/features/user/lib/user-parsers'
import {
  parseUserGroupDescription,
  parseUserGroupId,
  parseUserGroupName,
  parseUserGroupRole,
  parseUserGroupSlug,
  type ParseResult,
  userGroupDescriptionValue,
  userGroupNameValue,
  userGroupSlugValue,
} from '@/features/usergroup/lib/usergroup-parsers'
import type { AddUserGroupMemberRequest } from '@/features/usergroup/http/request/AddUserGroupMemberRequest'
import type { CreateUserGroupRequest } from '@/features/usergroup/http/request/CreateUserGroupRequest'
import type { UpdateUserGroupMemberRoleRequest } from '@/features/usergroup/http/request/UpdateUserGroupMemberRoleRequest'
import type { UpdateUserGroupRequest } from '@/features/usergroup/http/request/UpdateUserGroupRequest'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupListResponse } from '@/features/usergroup/http/response/UserGroupListResponse'
import type { UserGroupMember } from '@/features/usergroup/model/UserGroupMember'
import type { UserGroupSummary } from '@/features/usergroup/http/response/UserGroupSummary'

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type UserGroupRoleContract = 'owner' | 'manager' | 'member'
type AddUserGroupMemberRoleContract = 'manager' | 'member'

type UserGroupMemberContract = {
  username: string
  displayName: string
  role: UserGroupRoleContract
  joinedAt: string
}

type UserGroupSummaryContract = {
  id: string
  slug: string
  name: string
  description: string
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

type UserGroupDetailContract = {
  id: string
  slug: string
  name: string
  description: string
  ownerUsername: string
  members: UserGroupMemberContract[]
  createdAt: string
  updatedAt: string
}

type CreateUserGroupRequestContract = {
  slug: string
  name: string
  description: string
}

type UpdateUserGroupRequestContract = {
  name: string
  description: string
}

type AddUserGroupMemberRequestContract = {
  username: string
  role: AddUserGroupMemberRoleContract
}

type UpdateUserGroupMemberRoleRequestContract = {
  role: UserGroupRoleContract
}

type UserGroupListResponseContract = PageResponseContract<UserGroupSummaryContract>

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
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
