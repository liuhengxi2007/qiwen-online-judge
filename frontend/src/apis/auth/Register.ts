import type { APIMessage } from '@/system/api/api-message'
import type { RegisterRequest } from '@/objects/auth/request/RegisterRequest'
import type { RegisterResponse } from '@/objects/auth/response/RegisterResponse'
import { fromRegisterResponseContract } from '@/objects/auth/response/RegisterResponse'

export class Register implements APIMessage<RegisterResponse> {
  declare readonly responseType?: RegisterResponse
  readonly method = 'POST'
  readonly decode = fromRegisterResponseContract
  readonly apiPath = 'auth/register'
  private readonly request: RegisterRequest

  constructor(request: RegisterRequest) {
    this.request = request
  }

  body(): RegisterRequest {
    return this.request
  }
}
