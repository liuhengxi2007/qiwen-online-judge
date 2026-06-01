import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'

export class SubscribeNotificationEvents implements APIWithSessionMessage<void> {
  declare readonly responseType?: void
  readonly method = 'GET'
  readonly decode = () => undefined
  readonly apiPath = 'notifications/events'

  body(): undefined {
    return undefined
  }

  eventUrl(): string {
    return apiPath(this)
  }
}
