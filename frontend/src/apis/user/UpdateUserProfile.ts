import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 更新用户资料的请求联合；支持用户自助和管理员代管两种请求来源。 */
type UpdateUserProfileRequest = UpdateOwnProfileRequest | UpdateManagedUserProfileRequest

/** 更新指定用户资料；输入用户名和资料请求，输出新的会话设置快照。 */
export class UpdateUserProfile implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserProfileRequest

  constructor(username: Username, request: UpdateUserProfileRequest) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings/profile`
    this.request = request
  }

  body(): UpdateUserProfileRequest {
    return this.request
  }
}
