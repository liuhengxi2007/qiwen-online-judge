import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/usergroup-parsers'
import { fromUserGroupDetailContract } from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { usernameValue } from '@/objects/user/user-parsers'
import type { Username } from '@/objects/user/Username'
import { postJson } from '@/system/api/http-client'

export async function removeUserGroupMember(userGroupSlug: UserGroupSlug, targetUsername: Username): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/remove`,
    fromUserGroupDetailContract,
    {},
  )
}
