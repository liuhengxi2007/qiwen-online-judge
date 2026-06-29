import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 获取用户公开资料；输入用户名，输出公开 profile 响应，访问边界由后端处理。 */
export class GetUserProfile implements APIWithSessionMessage<UserProfileResponse> {
  declare readonly responseType?: UserProfileResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(username: Username) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/profile`
  }

  body(): undefined {
    return undefined
  }
}
