import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { fromSessionResponseContract } from '@/objects/auth/response/SessionResponse'

export class GetSession implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'GET'
  readonly decode = fromSessionResponseContract
  readonly apiPath = 'auth/session'

  body(): undefined {
    return undefined
  }
}
