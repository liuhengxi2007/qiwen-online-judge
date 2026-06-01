import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromUserProfileResponseContract } from '@/objects/user/response/UserProfileResponse'

export class GetUserProfile implements APIWithSessionMessage<UserProfileResponse> {
  declare readonly responseType?: UserProfileResponse
  readonly method = 'GET'
  readonly decode = fromUserProfileResponseContract
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/profile`
  }

  body(): undefined {
    return undefined
  }
}
