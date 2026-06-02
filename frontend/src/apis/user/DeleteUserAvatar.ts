import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserSettingsResponse } from '@/objects/user/response/UserSettingsResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class DeleteUserAvatar implements APIWithSessionMessage<UserSettingsResponse> {
  declare readonly responseType?: UserSettingsResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(targetUsername: Username) {
    this.apiPath = `users/${usernameValue(targetUsername)}/avatar/delete`
  }

  body(): undefined {
    return undefined
  }
}
