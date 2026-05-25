package domains.message.http

import domains.message.http.mapper.MessageHttpResponseMappers



import domains.message.application.MessageEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object MessageHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listInbox: Plain[domains.auth.model.AuthUser, shared.model.PageRequest, domains.message.model.response.MessageInboxResponse],
    getConversationHistory: Plain[domains.auth.model.AuthUser, MessageHttpPlans.HistoryInput, domains.message.application.MessageCommandResults.GetConversationHistoryResult],
    createConversation: WithTransaction[domains.auth.model.AuthUser, domains.message.model.request.CreateConversationRequest, domains.message.application.MessageCommandResults.CreateConversationResult],
    sendMessage: WithTransaction[domains.auth.model.AuthUser, (domains.message.model.MessageConversationId, domains.message.model.request.SendDirectMessageRequest), MessageHttpPlans.SendMessageOutput],
    markConversationRead: WithTransaction[domains.auth.model.AuthUser, (domains.message.model.MessageConversationId, domains.message.model.request.MarkConversationReadRequest), MessageHttpPlans.MarkConversationReadOutput],
    markAllMessagesRead: WithTransaction[domains.auth.model.AuthUser, Unit, domains.message.application.MessageCommandResults.MarkAllMessagesReadResult],
    listBlocks: Plain[domains.auth.model.AuthUser, Unit, List[domains.message.model.response.MessageBlockEntry]],
    addBlock: WithTransaction[domains.auth.model.AuthUser, domains.user.model.Username, domains.message.application.MessageCommandResults.AddBlockResult],
    removeBlock: WithTransaction[domains.auth.model.AuthUser, domains.user.model.Username, domains.message.application.MessageCommandResults.RemoveBlockResult]
  )

  def plans(messageEventHub: MessageEventHub): RegisteredPlans =
    RegisteredPlans(
      listInbox = Plain(MessageHttpPlans.ListInbox, MessageHttpResponseMappers.inboxResponse),
      getConversationHistory = Plain(MessageHttpPlans.GetConversationHistory, MessageHttpResponseMappers.historyResponse),
      createConversation = WithTransaction(MessageHttpPlans.CreateConversation, MessageHttpResponseMappers.createConversationResponse),
      sendMessage = WithTransaction(new MessageHttpPlans.SendMessagePlan(messageEventHub), MessageHttpResponseMappers.sendMessageResponse),
      markConversationRead = WithTransaction(new MessageHttpPlans.MarkConversationReadPlan(messageEventHub), MessageHttpResponseMappers.markConversationReadResponse),
      markAllMessagesRead = WithTransaction(new MessageHttpPlans.MarkAllMessagesReadPlan(messageEventHub), MessageHttpResponseMappers.markAllMessagesReadResponse),
      listBlocks = Plain(MessageHttpPlans.ListBlocks, MessageHttpResponseMappers.listBlocksResponse),
      addBlock = WithTransaction(new MessageHttpPlans.AddBlockPlan(messageEventHub), MessageHttpResponseMappers.addBlockResponse),
      removeBlock = WithTransaction(new MessageHttpPlans.RemoveBlockPlan(messageEventHub), MessageHttpResponseMappers.removeBlockResponse)
    )
