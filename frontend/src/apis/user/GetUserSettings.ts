import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 获取指定用户设置；输入用户名，输出会话形态的设置响应，需有效会话。 */
export class GetUserSettings implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings`
  }

  body(): undefined {
    return undefined
  }
}
