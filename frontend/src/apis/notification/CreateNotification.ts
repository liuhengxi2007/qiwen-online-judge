import type { APIMessage } from '@/system/api/api-message'
import type { CreateNotificationRequest } from '@/objects/notification/request/CreateNotificationRequest'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

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
