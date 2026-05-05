package domains.message.http

import domains.message.application.MessageEventHub
import domains.shared.http.AuthenticatedHttpPlanRegistry

object MessageHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listInbox: Plain[Unit, domains.message.model.MessageInboxResponse],
    getConversationHistory: Plain[MessageHttpPlans.HistoryInput, domains.message.application.MessageCommandResults.GetConversationHistoryResult],
    createConversation: WithTransaction[domains.message.model.CreateConversationRequest, domains.message.application.MessageCommandResults.CreateConversationResult],
    sendMessage: WithTransaction[(domains.message.model.MessageConversationId, domains.message.model.SendDirectMessageRequest), MessageHttpPlans.SendMessageOutput],
    markConversationRead: WithTransaction[(domains.message.model.MessageConversationId, domains.message.model.MarkConversationReadRequest), MessageHttpPlans.MarkConversationReadOutput],
    markAllMessagesRead: WithTransaction[Unit, domains.message.application.MessageCommandResults.MarkAllMessagesReadResult],
    listBlocks: Plain[Unit, List[domains.message.model.MessageBlockEntry]],
    addBlock: WithTransaction[domains.auth.model.Username, domains.message.application.MessageCommandResults.AddBlockResult],
    removeBlock: WithTransaction[domains.auth.model.Username, domains.message.application.MessageCommandResults.RemoveBlockResult]
  )

  def plans(messageEventHub: MessageEventHub): RegisteredPlans =
    RegisteredPlans(
      listInbox = Plain(MessageHttpPlans.ListInbox, MessageHttpResponses.inboxResponse),
      getConversationHistory = Plain(MessageHttpPlans.GetConversationHistory, MessageHttpResponses.historyResponse),
      createConversation = WithTransaction(MessageHttpPlans.CreateConversation, MessageHttpResponses.createConversationResponse),
      sendMessage = WithTransaction(new MessageHttpPlans.SendMessagePlan(messageEventHub), MessageHttpResponses.sendMessageResponse),
      markConversationRead = WithTransaction(new MessageHttpPlans.MarkConversationReadPlan(messageEventHub), MessageHttpResponses.markConversationReadResponse),
      markAllMessagesRead = WithTransaction(new MessageHttpPlans.MarkAllMessagesReadPlan(messageEventHub), MessageHttpResponses.markAllMessagesReadResponse),
      listBlocks = Plain(MessageHttpPlans.ListBlocks, MessageHttpResponses.listBlocksResponse),
      addBlock = WithTransaction(new MessageHttpPlans.AddBlockPlan(messageEventHub), MessageHttpResponses.addBlockResponse),
      removeBlock = WithTransaction(new MessageHttpPlans.RemoveBlockPlan(messageEventHub), MessageHttpResponses.removeBlockResponse)
    )
