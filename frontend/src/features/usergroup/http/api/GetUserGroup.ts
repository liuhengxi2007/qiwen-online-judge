import type {
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  userGroupSlugValue,
} from '@/features/usergroup/domain/usergroup'
import { requestJson } from '@/shared/api/http-client'

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}
