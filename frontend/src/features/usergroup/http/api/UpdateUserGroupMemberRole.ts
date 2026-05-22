import type {
  UpdateUserGroupMemberRoleRequest,
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import { userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  toUpdateUserGroupMemberRoleRequestContract,
} from '@/features/usergroup/http/codec'
import {
  usernameValue,
  type Username,
} from '@/features/user/domain/user'
import { postJson } from '@/shared/api/http-client'

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
