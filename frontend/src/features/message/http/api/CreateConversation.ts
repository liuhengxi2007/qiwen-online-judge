import type {
  CreateConversationRequest,
  MessageConversationSummary,
} from '@/features/message/domain/message'
import {
  fromMessageConversationSummary,
  toCreateConversationRequest,
} from '@/features/message/domain/message'
import { postJson } from '@/shared/api/http-client'

export function createConversation(request: CreateConversationRequest): Promise<MessageConversationSummary> {
  return postJson('/api/messages/conversations', fromMessageConversationSummary, toCreateConversationRequest(request))
}
