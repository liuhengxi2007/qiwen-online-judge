import type {
  AuthUserListItem,
  UpdateUserPermissionsRequest,
  Username,
} from '@/features/user/domain/user'
import { usernameValue } from '@/features/user/domain/user'
import {
  fromAuthUserListItemContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/user/http/codec'
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
