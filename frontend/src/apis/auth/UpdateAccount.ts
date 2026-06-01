import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromSessionResponseContract } from '@/objects/auth/response/SessionResponse'

type UpdateAccountRequest = UpdateOwnAccountRequest | UpdateManagedUserAccountRequest

export class UpdateAccount implements APIWithSessionMessage<SessionResponse> {
  declare readonly responseType?: SessionResponse
  readonly method = 'POST'
  readonly decode = fromSessionResponseContract
  readonly apiPath: string
  private readonly request: UpdateAccountRequest

  constructor(username: Username, request: UpdateAccountRequest) {
    this.apiPath = `auth/accounts/${encodeURIComponent(usernameValue(username))}/settings/account`
    this.request = request
  }

  body(): UpdateAccountRequest {
    return this.request
  }
}
