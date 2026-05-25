import type { AddUserGroupMemberRequest } from '@/features/usergroup/http/request/AddUserGroupMemberRequest'
import type { CreateUserGroupRequest } from '@/features/usergroup/http/request/CreateUserGroupRequest'
import type { UpdateUserGroupMemberRoleRequest } from '@/features/usergroup/http/request/UpdateUserGroupMemberRoleRequest'
import type { UpdateUserGroupRequest } from '@/features/usergroup/http/request/UpdateUserGroupRequest'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupListResponse } from '@/features/usergroup/http/response/UserGroupListResponse'
import type { UserGroupSummary } from '@/features/usergroup/http/response/UserGroupSummary'
import { toUsernameContract } from '@/features/user/http/codec/UserModelHttpCodecs'
import {
  fromUserGroupDescriptionContract,
  fromUserGroupIdContract,
  fromUserGroupMemberContract,
  fromUserGroupNameContract,
  fromUserGroupSlugContract,
  toAddUserGroupMemberRoleContract,
  toUserGroupDescriptionContract,
  toUserGroupNameContract,
  toUserGroupRoleContract,
  toUserGroupSlugContract,
  type AddUserGroupMemberRoleContract,
  type UserGroupMemberContract,
  type UserGroupRoleContract,
} from '@/features/usergroup/http/codec/UserGroupModelHttpCodecs'
import { fromUsernameContract } from '@/features/user/http/codec/UserModelHttpCodecs'

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
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

export function fromUserGroupSummaryContract(group: UserGroupSummaryContract): UserGroupSummary {
  return {
    id: fromUserGroupIdContract(group.id, 'user group summary id'),
    slug: fromUserGroupSlugContract(group.slug, 'user group summary slug'),
    name: fromUserGroupNameContract(group.name, 'user group summary name'),
    description: fromUserGroupDescriptionContract(group.description, 'user group summary description'),
    ownerUsername: fromUsernameContract(group.ownerUsername, 'user group summary owner username'),
    createdAt: group.createdAt,
    updatedAt: group.updatedAt,
  }
}

export function fromUserGroupDetailContract(group: UserGroupDetailContract): UserGroupDetail {
  return {
    id: fromUserGroupIdContract(group.id, 'user group detail id'),
    slug: fromUserGroupSlugContract(group.slug, 'user group detail slug'),
    name: fromUserGroupNameContract(group.name, 'user group detail name'),
    description: fromUserGroupDescriptionContract(group.description, 'user group detail description'),
    ownerUsername: fromUsernameContract(group.ownerUsername, 'user group detail owner username'),
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
    slug: toUserGroupSlugContract(request.slug),
    name: toUserGroupNameContract(request.name),
    description: toUserGroupDescriptionContract(request.description),
  }
}

export function toUpdateUserGroupRequestContract(request: UpdateUserGroupRequest): UpdateUserGroupRequestContract {
  return {
    name: toUserGroupNameContract(request.name),
    description: toUserGroupDescriptionContract(request.description),
  }
}

export function toAddUserGroupMemberRequestContract(
  request: AddUserGroupMemberRequest,
): AddUserGroupMemberRequestContract {
  return {
    username: toUsernameContract(request.username),
    role: toAddUserGroupMemberRoleContract(request.role),
  }
}

export function toUpdateUserGroupMemberRoleRequestContract(
  request: UpdateUserGroupMemberRoleRequest,
): UpdateUserGroupMemberRoleRequestContract {
  return {
    role: toUserGroupRoleContract(request.role),
  }
}
