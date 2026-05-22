import type {
  UpdateUserGroupRequest,
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  toUpdateUserGroupRequestContract,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup'
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
