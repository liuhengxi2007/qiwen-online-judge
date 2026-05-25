import type { UpdateUserGroupRequest } from '@/features/usergroup/model/request/UpdateUserGroupRequest'
import type { UserGroupDetail } from '@/features/usergroup/model/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import {
  fromUserGroupDetailContract,
  toUpdateUserGroupRequestContract,
} from '@/features/usergroup/http/codec/UserGroupHttpCodecs'
import { postJson } from '@/shared/api/http-client'

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
