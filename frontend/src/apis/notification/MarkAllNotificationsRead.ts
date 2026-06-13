import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 标记当前会话所有通知已读；无请求体，输出通用成功响应。 */
export class MarkAllNotificationsRead implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'notifications/mark-all-read'

  body(): undefined {
    return undefined
  }
}
