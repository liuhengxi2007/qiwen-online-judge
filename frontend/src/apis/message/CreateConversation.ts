import type { CreateConversationRequest } from '@/objects/message/request/CreateConversationRequest'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import {
  fromMessageConversationSummary,
  toCreateConversationRequest,
} from '@/apis/message/codecs/MessageHttpCodecs'
import { postJson } from '@/system/api/http-client'

export function createConversation(request: CreateConversationRequest): Promise<MessageConversationSummary> {
  return postJson('/api/messages/conversations', fromMessageConversationSummary, toCreateConversationRequest(request))
}
