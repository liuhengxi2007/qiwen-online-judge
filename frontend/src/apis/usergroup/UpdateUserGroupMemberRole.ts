import type { UpdateUserGroupMemberRoleRequest } from '@/objects/usergroup/request/UpdateUserGroupMemberRoleRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import {
  fromUserGroupDetailContract,
  toUpdateUserGroupMemberRoleRequestContract,
} from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { usernameValue } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import { postJson } from '@/system/api/http-client'

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
