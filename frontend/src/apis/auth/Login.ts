import type { APIMessage } from '@/system/api/api-message'
import type { LoginRequest } from '@/objects/auth/request/LoginRequest'
import type { LoginResponse } from '@/objects/auth/response/LoginResponse'

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
