import type { UpdateUserPermissionsRequest } from '@/features/auth/model/request/UpdateUserPermissionsRequest'
import type { Username } from '@/features/user/model/Username'
import type { AuthAccountListItem } from '@/features/auth/model/response/AuthAccountListItem'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  fromAuthAccountListItemContract,
  toUpdateUserPermissionsRequestContract,
} from '@/features/auth/http/codec/AuthHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function updateAccountPermissions(
  username: Username,
  request: UpdateUserPermissionsRequest,
): Promise<AuthAccountListItem> {
  return postJson(
    `/api/auth/accounts/${encodeURIComponent(usernameValue(username))}/permissions`,
    fromAuthAccountListItemContract,
    toUpdateUserPermissionsRequestContract(request),
  )
}
