import type { APIMessage } from '@/system/api/api-message'
import type { LoginRequest } from '@/objects/auth/request/LoginRequest'
import type { LoginResponse } from '@/objects/auth/response/LoginResponse'

/** 登录 API 消息；提交用户名和明文密码，成功后由后端建立会话并返回用户资料。 */
export class Login implements APIMessage<LoginResponse> {
  declare readonly responseType?: LoginResponse
  readonly method = 'POST'
  readonly apiPath = 'auth/login'
  private readonly request: LoginRequest

  constructor(request: LoginRequest) {
    this.request = request
  }

  body(): LoginRequest {
    return this.request
  }
}
