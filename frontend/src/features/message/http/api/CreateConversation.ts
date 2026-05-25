import type { CreateConversationRequest } from '@/features/message/model/request/CreateConversationRequest'
import type { MessageConversationSummary } from '@/features/message/model/response/MessageConversationSummary'
import {
  fromMessageConversationSummary,
  toCreateConversationRequest,
} from '@/features/message/http/codec/MessageHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function createConversation(request: CreateConversationRequest): Promise<MessageConversationSummary> {
  return postJson('/api/messages/conversations', fromMessageConversationSummary, toCreateConversationRequest(request))
}
