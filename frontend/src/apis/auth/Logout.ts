import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import { decodeSuccessResponse } from '@/system/api/http-client'

export class Logout implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly decode = decodeSuccessResponse
  readonly apiPath = 'auth/logout'

  body(): undefined {
    return undefined
  }
}
