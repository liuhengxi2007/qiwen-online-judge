import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { AuthAccountListItem } from '@/objects/auth/response/AuthAccountListItem'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 更新账号权限位；输入目标用户名和权限请求，输出更新后的账号列表项。 */
export class UpdateAccountPermissions implements APIWithSessionMessage<AuthAccountListItem> {
  declare readonly responseType?: AuthAccountListItem
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserPermissionsRequest

  constructor(username: Username, request: UpdateUserPermissionsRequest) {
    this.apiPath = `auth/accounts/${encodeURIComponent(usernameValue(username))}/permissions`
    this.request = request
  }

  body(): UpdateUserPermissionsRequest {
    return this.request
  }
}
