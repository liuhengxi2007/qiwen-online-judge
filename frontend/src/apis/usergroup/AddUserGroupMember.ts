import type { AddUserGroupMemberRequest } from '@/objects/usergroup/request/AddUserGroupMemberRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import {
  fromUserGroupDetailContract,
  toAddUserGroupMemberRequestContract,
} from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
