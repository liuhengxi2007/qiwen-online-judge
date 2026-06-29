import type { APIMessage } from '@/system/api/api-message'
import type { RegisterRequest } from '@/objects/auth/request/RegisterRequest'
import type { RegisterResponse } from '@/objects/auth/response/RegisterResponse'

/** 注册 API 消息；提交新账号资料和明文密码，成功后返回新账号会话资料。 */
export class Register implements APIMessage<RegisterResponse> {
  declare readonly responseType?: RegisterResponse
  readonly method = 'POST'
  readonly apiPath = 'auth/register'
  private readonly request: RegisterRequest

  constructor(request: RegisterRequest) {
    this.request = request
  }

  body(): RegisterRequest {
    return this.request
  }
}
