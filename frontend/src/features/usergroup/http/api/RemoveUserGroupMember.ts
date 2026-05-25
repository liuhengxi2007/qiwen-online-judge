import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import { fromUserGroupDetailContract } from '@/features/usergroup/http/codec/UserGroupHttpCodecs'
import { usernameValue } from '@/features/user/lib/user-parsers'
import type { Username } from '@/features/user/model/Username'
import { postJson } from '@/shared/api/http-client'

export async function removeUserGroupMember(userGroupSlug: UserGroupSlug, targetUsername: Username): Promise<UserGroupDetail> {
  return postJson(
    `/api/user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/remove`,
    fromUserGroupDetailContract,
    {},
  )
}
