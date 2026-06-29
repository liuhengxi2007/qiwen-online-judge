import type { APIMessage } from '@/system/api/api-message'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import type { UserProfileSettings } from '@/objects/user/UserProfileSettings'

/** 查找用户资料设置的内部 API；输入用户名，输出设置快照或空值。 */
export class FindUserProfileSettings implements APIMessage<UserProfileSettings | null> {
  declare readonly responseType?: UserProfileSettings | null
  readonly method = 'GET'
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `internal/users/${encodeURIComponent(usernameValue(username))}/profile-settings`
  }

  body(): undefined {
    return undefined
  }
}
