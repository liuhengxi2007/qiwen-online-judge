import type { UserGroupDetail } from '@/features/usergroup/model/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import { fromUserGroupDetailContract } from '@/features/usergroup/http/codec/UserGroupHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}
