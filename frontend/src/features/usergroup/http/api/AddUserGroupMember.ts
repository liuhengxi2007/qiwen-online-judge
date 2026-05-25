import type { AddUserGroupMemberRequest } from '@/features/usergroup/model/request/AddUserGroupMemberRequest'
import type { UserGroupDetail } from '@/features/usergroup/model/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import {
  fromUserGroupDetailContract,
  toAddUserGroupMemberRequestContract,
} from '@/features/usergroup/http/codec/UserGroupHttpCodecs'
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
