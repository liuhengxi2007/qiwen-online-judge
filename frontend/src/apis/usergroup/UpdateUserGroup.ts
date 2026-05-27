import type { UpdateUserGroupRequest } from '@/objects/usergroup/request/UpdateUserGroupRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import {
  fromUserGroupDetailContract,
  toUpdateUserGroupRequestContract,
} from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
