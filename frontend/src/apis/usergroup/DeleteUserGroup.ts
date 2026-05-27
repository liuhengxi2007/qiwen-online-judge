import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/usergroup-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'

export function deleteUserGroup(userGroupSlug: UserGroupSlug): Promise<SuccessResponse> {
  return postJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}/delete`, decodeSuccessResponse, {})
}
