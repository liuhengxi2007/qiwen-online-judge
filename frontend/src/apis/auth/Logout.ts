import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 登出当前会话；无请求体，后端负责清理会话状态并返回通用成功响应。 */
export class Logout implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'auth/logout'

  body(): undefined {
    return undefined
  }
}
