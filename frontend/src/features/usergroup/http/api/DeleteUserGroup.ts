import type { SuccessResponse } from '@contracts/shared'
import type { UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteUserGroup(userGroupSlug: UserGroupSlug): Promise<SuccessResponse> {
  return postJson(`/api/user-groups/${userGroupSlugValue(userGroupSlug)}/delete`, decodeSuccessResponse, {})
}
