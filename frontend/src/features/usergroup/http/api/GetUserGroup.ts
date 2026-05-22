import type {
  UserGroupDetail,
  UserGroupSlug,
} from '@/features/usergroup/domain/usergroup'
import { userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import { fromUserGroupDetailContract } from '@/features/usergroup/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}
