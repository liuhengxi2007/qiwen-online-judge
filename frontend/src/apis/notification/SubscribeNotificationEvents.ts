import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'

/** 订阅通知事件流；无请求体，eventUrl 生成带基础 API 前缀的 SSE URL。 */
export class SubscribeNotificationEvents implements APIWithSessionMessage<void> {
  declare readonly responseType?: void
  readonly method = 'GET'
  readonly apiPath = 'notifications/events'

  body(): undefined {
    return undefined
  }

  eventUrl(): string {
    return apiPath(this)
  }
}
