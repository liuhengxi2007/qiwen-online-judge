import type { MessageId } from '@/objects/message/MessageId'
import type { MarkConversationReadMode } from '@/objects/message/request/MarkConversationReadMode'

/** 标记会话已读请求；按模式决定是否需要提供消息游标。 */
export type MarkConversationReadRequest =
  | {
      mode: Extract<MarkConversationReadMode, 'conversation'>
    }
  | {
      mode: Extract<MarkConversationReadMode, 'message'>
      messageId: MessageId
    }
