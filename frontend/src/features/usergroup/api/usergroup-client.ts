import type { SuccessResponse } from '@contracts/shared'
import type {
  AddUserGroupMemberRequest,
  CreateUserGroupRequest,
  UpdateUserGroupMemberRoleRequest,
  UpdateUserGroupRequest,
  UserGroupDetail,
  UserGroupListResponse,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  fromUserGroupListResponseContract,
  toAddUserGroupMemberRequestContract,
  toCreateUserGroupRequestContract,
  toUpdateUserGroupMemberRoleRequestContract,
  toUpdateUserGroupRequestContract,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup'
import { usernameValue, type Username } from '@/features/auth/domain/auth'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

export async function listUserGroups(pageRequest?: PageRequest): Promise<UserGroupListResponse> {
  const url = new URL('/api/user-groups', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromUserGroupListResponseContract)
}

export async function createUserGroup(request: CreateUserGroupRequest): Promise<UserGroupDetail> {
  return postJson('/api/user-groups', fromUserGroupDetailContract, toCreateUserGroupRequestContract(request))
}

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}

export async function updateUserGroup(
  userGroupSlug: UserGroupSlug,
  request: UpdateUserGroupRequest,
): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}`,
    fromUserGroupDetailContract,
    toUpdateUserGroupRequestContract(request),
  )
}

export function deleteUserGroup(userGroupSlug: UserGroupSlug): Promise<SuccessResponse> {
  return postJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}/delete`, decodeSuccessResponse, {})
}

export async function addUserGroupMember(
  userGroupSlug: UserGroupSlug,
  request: AddUserGroupMemberRequest,
): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members`,
    fromUserGroupDetailContract,
    toAddUserGroupMemberRequestContract(request),
  )
}

export async function updateUserGroupMemberRole(
  userGroupSlug: UserGroupSlug,
  targetUsername: Username,
  request: UpdateUserGroupMemberRoleRequest,
): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/role`,
    fromUserGroupDetailContract,
    toUpdateUserGroupMemberRoleRequestContract(request),
  )
}

export async function removeUserGroupMember(userGroupSlug: UserGroupSlug, targetUsername: Username): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/remove`,
    fromUserGroupDetailContract,
    {},
  )
}
