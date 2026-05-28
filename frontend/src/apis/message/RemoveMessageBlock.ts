import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class RemoveMessageBlock implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(targetUsername: Username) {
    this.apiPath = `messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/remove`
  }

  body(): undefined {
    return undefined
  }
}
