import type { APIMessage } from '@/system/api/api-message'
import type { ResolveAccountUsernameResponse } from '@/objects/auth/response/ResolveAccountUsernameResponse'
import type { Username } from '@/objects/user/Username'

type ResolveAccountUsernameBody = {
  username: Username
}

export class ResolveAccountUsername implements APIMessage<ResolveAccountUsernameResponse> {
  declare readonly responseType?: ResolveAccountUsernameResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/auth/resolve-account-username'
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
  }

  body(): ResolveAccountUsernameBody {
    return { username: this.username }
  }
}
