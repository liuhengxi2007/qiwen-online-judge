import type { APIMessage } from '@/system/api/api-message'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import type { UserProfileSettings } from '@/objects/user/UserProfileSettings'
import { readNullable } from '@/objects/shared/PageResponse'
import { fromUserProfileSettingsContract } from '@/objects/user/UserProfileSettings'

export class FindUserProfileSettings implements APIMessage<UserProfileSettings | null> {
  declare readonly responseType?: UserProfileSettings | null
  readonly method = 'GET'
  readonly decode = (value: unknown) => readNullable(value, 'user profile settings', fromUserProfileSettingsContract)
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `internal/users/${encodeURIComponent(usernameValue(username))}/profile-settings`
  }

  body(): undefined {
    return undefined
  }
}
