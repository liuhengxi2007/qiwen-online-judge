import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import { fromUserGroupDetailContract } from '@/features/usergroup/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}
