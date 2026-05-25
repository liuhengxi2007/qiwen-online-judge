package domains.message.http.mapper

import domains.message.http.MessageHttpPlans
import domains.message.model.{MessageConversationId, MessageId}
import domains.message.model.request.{CreateConversationRequest, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.user.model.Username
import shared.http.utils.PageRequestQuerySupport
import shared.model.PageRequest

object MessageHttpRequestMappers:

  def username(rawUsername: String): Username =
    Username.canonical(rawUsername)

  def createConversationRequest(body: CreateConversationRequest): CreateConversationRequest =
    body

  def inboxRequest(queryParams: Map[String, String]): PageRequest =
    PageRequestQuerySupport.parsePageRequest(queryParams)

  def conversationId(rawConversationId: String): Either[String, MessageConversationId] =
    MessageConversationId.parse(rawConversationId)

  def historyInput(rawConversationId: String, queryParams: Map[String, String]): Either[String, MessageHttpPlans.HistoryInput] =
    MessageConversationId.parse(rawConversationId).map { conversationId =>
      MessageHttpPlans.HistoryInput(
        conversationId,
        queryParams.get("before").flatMap(rawId => MessageId.parse(rawId).toOption),
        queryParams.get("limit").flatMap(_.toIntOption)
      )
    }

  def markConversationReadInput(
    rawConversationId: String,
    body: MarkConversationReadRequest
  ): Either[String, (MessageConversationId, MarkConversationReadRequest)] =
    MessageConversationId.parse(rawConversationId).map(conversationId => (conversationId, body))

  def sendMessageInput(
    rawConversationId: String,
    body: SendDirectMessageRequest
  ): Either[String, (MessageConversationId, SendDirectMessageRequest)] =
    MessageConversationId.parse(rawConversationId).map(conversationId => (conversationId, body))
