import type { APIMessage } from '@/system/api/api-message'
import type { CreateNotificationRequest } from '@/objects/notification/request/CreateNotificationRequest'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'

/** 内部创建通知 API；输入完整通知请求体，输出通用成功响应。 */
export class CreateNotification implements APIMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/notifications'
  private readonly request: CreateNotificationRequest

  constructor(request: CreateNotificationRequest) {
    this.request = request
  }

  body(): CreateNotificationRequest {
    return this.request
  }
}
