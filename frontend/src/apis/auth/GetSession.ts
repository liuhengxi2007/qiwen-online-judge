import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'

/** 获取当前登录会话；无请求体，输出当前用户资料和权限，依赖浏览器会话凭据。 */
export class GetSession implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'GET'
  readonly apiPath = 'auth/session'

  body(): undefined {
    return undefined
  }
}
