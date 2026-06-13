import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { notificationIdValue } from '@/objects/notification/NotificationId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 标记单条通知已读；输入通知 ID，输出通用成功响应。 */
export class MarkNotificationRead implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(notificationId: NotificationId) {
    this.apiPath = `notifications/${encodeURIComponent(notificationIdValue(notificationId))}/mark-read`
  }

  body(): undefined {
    return undefined
  }
}
