import type {
  CreateConversationRequest,
  DirectMessage,
  MarkConversationReadRequest,
  MessageBlockEntry,
  MessageConversationId,
  MessageConversationSummary,
  MessageHistoryResponse,
  MessageId,
  MessageInboxResponse,
  SendDirectMessageRequest,
  Username,
} from '@/features/message/domain/message'
import {
  fromDirectMessage,
  fromMessageBlockEntry,
  fromMessageConversationSummary,
  fromMessageHistoryResponse,
  fromMessageInboxResponse,
  messageConversationIdValue,
  messageIdValue,
  toCreateConversationRequest,
  toMarkConversationReadRequest,
  toSendDirectMessageRequest,
} from '@/features/message/domain/message'
import { usernameValue } from '@/features/auth/domain/auth'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'
import type { SuccessResponse } from '@contracts/shared'
import type { PageRequest } from '@/shared/model/Pagination'

export function listInbox(pageRequest?: PageRequest): Promise<MessageInboxResponse> {
  const url = new URL('/api/messages/inbox', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromMessageInboxResponse)
}

export function createConversation(request: CreateConversationRequest): Promise<MessageConversationSummary> {
  return postJson('/api/messages/conversations', fromMessageConversationSummary, toCreateConversationRequest(request))
}

export function getConversationHistory(
  conversationId: MessageConversationId,
  options: { beforeMessageId?: MessageId | null; limit?: number } = {},
): Promise<MessageHistoryResponse> {
  const url = new URL(`/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages`, window.location.origin)
  if (options.beforeMessageId) {
    url.searchParams.set('before', messageIdValue(options.beforeMessageId))
  }
  if (options.limit) {
    url.searchParams.set('limit', String(options.limit))
  }

  return requestJson(url.pathname + url.search, fromMessageHistoryResponse)
}

export function sendDirectMessage(
  conversationId: MessageConversationId,
  request: SendDirectMessageRequest,
): Promise<DirectMessage> {
  return postJson(
    `/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages`,
    fromDirectMessage,
    toSendDirectMessageRequest(request),
  )
}

export function markConversationRead(
  conversationId: MessageConversationId,
  request: MarkConversationReadRequest,
): Promise<MessageConversationSummary> {
  return postJson(
    `/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/read`,
    fromMessageConversationSummary,
    toMarkConversationReadRequest(request),
  )
}

export function markAllMessagesRead(): Promise<SuccessResponse> {
  return postJson('/api/messages/read-all', decodeSuccessResponse, {})
}

export function listMessageBlocks(): Promise<MessageBlockEntry[]> {
  return requestJson('/api/messages/blocks', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid message blocks payload.')
    }

    return value.map(fromMessageBlockEntry)
  })
}

export function addMessageBlock(targetUsername: Username): Promise<MessageBlockEntry> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`,
    fromMessageBlockEntry,
    {},
  )
}

export function removeMessageBlock(targetUsername: Username): Promise<SuccessResponse> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/remove`,
    decodeSuccessResponse,
    {},
  )
}
