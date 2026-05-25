import type { AuthUserListItem } from '@/features/user/model/response/AuthUserListItem'
import type { UpdateUserPermissionsRequest } from '@/features/user/model/request/UpdateUserPermissionsRequest'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  fromAuthUserListItemContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/user/http/codec/UserHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function updateUserPermissions(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthUserListItem> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/permissions`,
    fromAuthUserListItemContract,
    toUpdateUserPermissionsRequestContract(request),
  )
}
