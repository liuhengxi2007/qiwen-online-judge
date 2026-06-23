import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'

/** 订阅合并实时事件流；不需要请求体。 */
export class SubscribeRealtimeEvents implements APIWithSessionMessage<void> {
  declare readonly responseType?: void
  readonly method = 'GET'
  readonly apiPath = 'realtime/events'

  body(): undefined {
    return undefined
  }

  eventUrl(): string {
    return apiPath(this)
  }
}
