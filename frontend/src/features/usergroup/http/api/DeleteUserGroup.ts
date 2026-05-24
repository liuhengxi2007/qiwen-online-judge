import type { SuccessResponse } from '@/shared/http/response/SuccessResponse'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { userGroupSlugValue } from '@/features/usergroup/lib/usergroup-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteUserGroup(userGroupSlug: UserGroupSlug): Promise<SuccessResponse> {
  return postJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}/delete`, decodeSuccessResponse, {})
}
