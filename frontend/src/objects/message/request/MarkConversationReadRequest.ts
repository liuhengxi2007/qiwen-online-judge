import type { MessageId } from '@/objects/message/MessageId'
import type { MarkConversationReadMode } from '@/objects/message/request/MarkConversationReadMode'

export type MarkConversationReadRequest =
  | {
      mode: Extract<MarkConversationReadMode, 'conversation'>
    }
  | {
      mode: Extract<MarkConversationReadMode, 'message'>
      messageId: MessageId
    }
