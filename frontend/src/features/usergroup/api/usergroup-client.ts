import type {
  UserGroupDetail as UserGroupDetailContract,
  UserGroupListResponse as UserGroupListResponseContract,
} from '@contracts/usergroup'
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
import { postJson, requestJson } from '@/shared/api/http-client'

export async function listUserGroups(): Promise<UserGroupListResponse> {
  const response = await requestJson<UserGroupListResponseContract>('/api/user-groups')
  return fromUserGroupListResponseContract(response)
}

export async function createUserGroup(request: CreateUserGroupRequest): Promise<UserGroupDetail> {
  const response = await postJson<UserGroupDetailContract>('/api/user-groups', toCreateUserGroupRequestContract(request))
  return fromUserGroupDetailContract(response)
}

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  const response = await requestJson<UserGroupDetailContract>(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`)
  return fromUserGroupDetailContract(response)
}

export async function updateUserGroup(
  userGroupSlug: UserGroupSlug,
  request: UpdateUserGroupRequest,
): Promise<UserGroupDetail> {
  const response = await postJson<UserGroupDetailContract>(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}`,
    toUpdateUserGroupRequestContract(request),
  )
  return fromUserGroupDetailContract(response)
}

export function deleteUserGroup(userGroupSlug: UserGroupSlug): Promise<SuccessResponse> {
  return postJson<SuccessResponse>(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}/delete`, {})
}

export async function addUserGroupMember(
  userGroupSlug: UserGroupSlug,
  request: AddUserGroupMemberRequest,
): Promise<UserGroupDetail> {
  const response = await postJson<UserGroupDetailContract>(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members`,
    toAddUserGroupMemberRequestContract(request),
  )
  return fromUserGroupDetailContract(response)
}

export async function updateUserGroupMemberRole(
  userGroupSlug: UserGroupSlug,
  targetUsername: Username,
  request: UpdateUserGroupMemberRoleRequest,
): Promise<UserGroupDetail> {
  const response = await postJson<UserGroupDetailContract>(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/role`,
    toUpdateUserGroupMemberRoleRequestContract(request),
  )
  return fromUserGroupDetailContract(response)
}
