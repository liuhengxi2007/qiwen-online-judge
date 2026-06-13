import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 更新账号请求联合；区分用户自助改账号和管理员代管账号两种 body。 */
type UpdateAccountRequest = UpdateOwnAccountRequest | UpdateManagedUserAccountRequest

/** 更新指定账号设置；输入目标用户名和请求体，输出新的会话快照，权限由后端区分。 */
export class UpdateAccount implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateAccountRequest

  constructor(username: Username, request: UpdateAccountRequest) {
    this.apiPath = `auth/accounts/${encodeURIComponent(usernameValue(username))}/settings/account`
    this.request = request
  }

  body(): UpdateAccountRequest {
    return this.request
  }
}
