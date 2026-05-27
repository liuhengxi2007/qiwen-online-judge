import type { MessageId } from '@/objects/message/MessageId'

export type MarkConversationReadRequest =
  | {
      mode: 'conversation'
    }
  | {
      mode: 'message'
      messageId: MessageId
    }
