package domains.message.http

import domains.message.http.mapper.MessageHttpResponseMappers



import domains.message.application.MessageEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object MessageHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listInbox: Plain[domains.auth.objects.AuthUser, shared.objects.PageRequest, domains.message.objects.response.MessageInboxResponse],
    getConversationHistory: Plain[domains.auth.objects.AuthUser, MessageHttpPlans.HistoryInput, domains.message.application.MessageCommandResults.GetConversationHistoryResult],
    createConversation: WithTransaction[domains.auth.objects.AuthUser, domains.message.objects.request.CreateConversationRequest, domains.message.application.MessageCommandResults.CreateConversationResult],
    sendMessage: WithTransaction[domains.auth.objects.AuthUser, (domains.message.objects.MessageConversationId, domains.message.objects.request.SendDirectMessageRequest), MessageHttpPlans.SendMessageOutput],
    markConversationRead: WithTransaction[domains.auth.objects.AuthUser, (domains.message.objects.MessageConversationId, domains.message.objects.request.MarkConversationReadRequest), MessageHttpPlans.MarkConversationReadOutput],
    markAllMessagesRead: WithTransaction[domains.auth.objects.AuthUser, Unit, domains.message.application.MessageCommandResults.MarkAllMessagesReadResult],
    listBlocks: Plain[domains.auth.objects.AuthUser, Unit, List[domains.message.objects.response.MessageBlockEntry]],
    addBlock: WithTransaction[domains.auth.objects.AuthUser, domains.user.objects.Username, domains.message.application.MessageCommandResults.AddBlockResult],
    removeBlock: WithTransaction[domains.auth.objects.AuthUser, domains.user.objects.Username, domains.message.application.MessageCommandResults.RemoveBlockResult]
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
