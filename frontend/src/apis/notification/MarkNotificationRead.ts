import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { notificationIdValue } from '@/objects/notification/NotificationId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import { decodeSuccessResponse } from '@/system/api/http-client'

export class MarkNotificationRead implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly decode = decodeSuccessResponse
  readonly apiPath: string

  constructor(notificationId: NotificationId) {
    this.apiPath = `notifications/${encodeURIComponent(notificationIdValue(notificationId))}/mark-read`
  }

  body(): undefined {
    return undefined
  }
}
