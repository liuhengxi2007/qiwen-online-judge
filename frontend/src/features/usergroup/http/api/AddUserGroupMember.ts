import type {
  AddUserGroupMemberRequest,
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import { userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  toAddUserGroupMemberRequestContract,
} from '@/features/usergroup/http/codec'
import { postJson } from '@/shared/api/http-client'

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
