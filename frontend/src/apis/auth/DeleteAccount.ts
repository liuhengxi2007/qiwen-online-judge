import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class DeleteAccount implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `auth/accounts/${encodeURIComponent(usernameValue(username))}/delete`
  }

  body(): undefined {
    return undefined
  }
}
