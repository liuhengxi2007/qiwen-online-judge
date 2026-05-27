import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { Username } from '@/objects/user/Username'
import type { AuthAccountListItem } from '@/objects/auth/response/AuthAccountListItem'
import { usernameValue } from '@/objects/user/Username'
import {
  fromAuthAccountListItemContract,
  toUpdateUserPermissionsRequestContract,
} from '@/apis/auth/codecs/AuthHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
