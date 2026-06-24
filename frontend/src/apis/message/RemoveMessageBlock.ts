import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 移除消息屏蔽对象；输入目标用户名，输出通用成功响应。 */
export class RemoveMessageBlock implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(targetUsername: Username) {
    this.apiPath = `messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/unlink`
  }

  body(): undefined {
    return undefined
  }
}
