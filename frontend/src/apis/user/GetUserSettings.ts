import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromSessionResponseContract } from '@/objects/auth/response/SessionResponse'

export class GetUserSettings implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'GET'
  readonly decode = fromSessionResponseContract
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/settings`
  }

  body(): undefined {
    return undefined
  }
}
