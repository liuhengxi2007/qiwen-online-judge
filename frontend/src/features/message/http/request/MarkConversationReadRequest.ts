import type { MessageId } from '@/features/message/model/MessageId'

export type MarkConversationReadRequest =
  | {
      mode: 'conversation'
    }
  | {
      mode: 'message'
      messageId: MessageId
    }
