import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 删除指定账号的会话 API 消息；输入目标用户名，输出通用成功响应，权限由后端校验。 */
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
