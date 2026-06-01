import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'

export class SubscribeMessageEvents implements APIWithSessionMessage<void> {
  declare readonly responseType?: void
  readonly method = 'GET'
  readonly decode = () => undefined
  readonly apiPath = 'messages/events'

  body(): undefined {
    return undefined
  }

  eventUrl(): string {
    return apiPath(this)
  }
}
