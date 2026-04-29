package domains.message.http

import domains.message.application.MessageEventHub
import domains.shared.http.AuthenticatedHttpPlanRegistry

object MessageHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  def plans(messageEventHub: MessageEventHub): Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    val listInbox = Plain(MessageHttpPlans.ListInbox, MessageHttpResponses.inboxResponse)
    val getConversationHistory = Plain(MessageHttpPlans.GetConversationHistory, MessageHttpResponses.historyResponse)
    val createConversation = WithTransaction(MessageHttpPlans.CreateConversation, MessageHttpResponses.createConversationResponse)
    val sendMessage = WithTransaction(new MessageHttpPlans.SendMessagePlan(messageEventHub), MessageHttpResponses.sendMessageResponse)
    val markConversationRead =
      WithTransaction(new MessageHttpPlans.MarkConversationReadPlan(messageEventHub), MessageHttpResponses.markConversationReadResponse)
    val markAllMessagesRead =
      WithTransaction(new MessageHttpPlans.MarkAllMessagesReadPlan(messageEventHub), MessageHttpResponses.markAllMessagesReadResponse)
    val listBlocks = Plain(MessageHttpPlans.ListBlocks, MessageHttpResponses.listBlocksResponse)
    val addBlock = WithTransaction(new MessageHttpPlans.AddBlockPlan(messageEventHub), MessageHttpResponses.addBlockResponse)
    val removeBlock = WithTransaction(new MessageHttpPlans.RemoveBlockPlan(messageEventHub), MessageHttpResponses.removeBlockResponse)

    List(
      listInbox,
      getConversationHistory,
      createConversation,
      sendMessage,
      markConversationRead,
      markAllMessagesRead,
      listBlocks,
      addBlock,
      removeBlock
    ).map(plan => plan.name -> plan).toMap
