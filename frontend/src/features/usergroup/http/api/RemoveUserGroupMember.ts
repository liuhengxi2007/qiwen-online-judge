import type {
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup'
import {
  usernameValue,
  type Username,
} from '@/features/user/domain/user'
import { postJson } from '@/shared/api/http-client'

export async function removeUserGroupMember(userGroupSlug: UserGroupSlug, targetUsername: Username): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/remove`,
    fromUserGroupDetailContract,
    {},
  )
}
