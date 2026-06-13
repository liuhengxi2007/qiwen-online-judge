import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'

/** 订阅私信事件流；无请求体，eventUrl 生成带基础 API 前缀的 SSE URL。 */
export class SubscribeMessageEvents implements APIWithSessionMessage<void> {
  declare readonly responseType?: void
  readonly method = 'GET'
  readonly apiPath = 'messages/events'

  body(): undefined {
    return undefined
  }

  eventUrl(): string {
    return apiPath(this)
  }
}
