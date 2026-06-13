import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 更新用户偏好的请求联合；支持用户自助和管理员代管两种请求来源。 */
type UpdateUserPreferencesRequest = UpdateOwnPreferencesRequest | UpdateManagedUserPreferencesRequest

/** 更新指定用户偏好；输入用户名和偏好请求，输出新的会话设置快照。 */
export class UpdateUserPreferences implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserPreferencesRequest

  constructor(username: Username, request: UpdateUserPreferencesRequest) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings/preferences`
    this.request = request
  }

  body(): UpdateUserPreferencesRequest {
    return this.request
  }
}
