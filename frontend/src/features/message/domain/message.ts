export type { ParseResult } from '@/features/auth/domain/auth'
export {
  fromDirectMessage,
  fromMessageBlockEntry,
  fromMessageConversationSummary,
  fromMessageHistoryResponse,
  fromMessageInboxResponse,
  messageContentValue,
  messageConversationIdValue,
  messageIdValue,
  parseMessageContent,
  parseMessageConversationId,
  parseMessageId,
  toCreateConversationRequest,
  toMarkConversationReadRequest,
  toSendDirectMessageRequest,
} from '@/features/message/domain/message-parsers'

export type { Username } from '@/features/auth/domain/auth'
export type { CreateConversationRequest } from '@/features/message/model/CreateConversationRequest'
export type { ConversationMessageFacts } from '@/features/message/model/ConversationMessageFacts'
export type { DirectMessage } from '@/features/message/model/DirectMessage'
export type { MarkConversationReadRequest } from '@/features/message/model/MarkConversationReadRequest'
export type { MessageBlockEntry } from '@/features/message/model/MessageBlockEntry'
export type { MessageContent } from '@/features/message/model/MessageContent'
export type { MessageConversationId } from '@/features/message/model/MessageConversationId'
export type { MessageConversationSummary } from '@/features/message/model/MessageConversationSummary'
export type { MessageHistoryResponse } from '@/features/message/model/MessageHistoryResponse'
export type { MessageId } from '@/features/message/model/MessageId'
export type { MessageInboxResponse } from '@/features/message/model/MessageInboxResponse'
export type { SendDirectMessageRequest } from '@/features/message/model/SendDirectMessageRequest'
