import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/usergroup-parsers'
import { fromUserGroupDetailContract } from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getUserGroup(userGroupSlug: UserGroupSlug): Promise<UserGroupDetail> {
  return requestJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}`, fromUserGroupDetailContract)
}
