import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'

/** 查询当前会话消息屏蔽列表；无请求体，输出被屏蔽用户条目数组。 */
export class ListMessageBlocks implements APIWithSessionMessage<MessageBlockEntry[]> {
  declare readonly responseType?: MessageBlockEntry[]
  readonly method = 'GET'
  readonly apiPath = 'messages/blocks'

  body(): undefined {
    return undefined
  }
}
