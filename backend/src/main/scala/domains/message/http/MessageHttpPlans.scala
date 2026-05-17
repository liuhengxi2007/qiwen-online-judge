package domains.message.http

import cats.syntax.all.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, MarkAllMessagesReadResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.application.{JdbcMessageRepository, MessageCommands, MessageEventHub, MessageStreamEvent}
import domains.message.model.{CreateConversationRequest, MarkConversationReadRequest, MessageConversationId, MessageId, SendDirectMessageRequest}
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.shared.model.PageRequest

import java.sql.Connection

object MessageHttpPlans:

  case object ListInbox extends PlainAuthenticatedHttpPlan[PageRequest, domains.message.model.MessageInboxResponse]:
    override val name: String = "ListInbox"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: PageRequest): IO[domains.message.model.MessageInboxResponse] =
      MessageCommands.listInbox(databaseSession, actor, input, JdbcMessageRepository)

  final case class HistoryInput(
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  )

  case object GetConversationHistory extends PlainAuthenticatedHttpPlan[HistoryInput, GetConversationHistoryResult]:
    override val name: String = "GetConversationHistory"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: HistoryInput): IO[GetConversationHistoryResult] =
      MessageCommands.getConversationHistory(databaseSession, actor, input.conversationId, input.beforeMessageId, input.limit, JdbcMessageRepository)

  case object CreateConversation extends TransactionAuthenticatedHttpPlan[CreateConversationRequest, CreateConversationResult]:
    override val name: String = "CreateConversation"
    override def execute(connection: Connection, actor: AuthUser, input: CreateConversationRequest): IO[CreateConversationResult] =
      MessageCommands.createConversation(connection, actor, input, JdbcMessageRepository)

  final case class SendMessageOutput(
    result: SendMessageResult,
    notification: Option[(Username, MessageStreamEvent.MessageReceived)]
  )

  final class SendMessagePlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[(MessageConversationId, SendDirectMessageRequest), SendMessageOutput]:
    override val name: String = "SendMessage"
    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (MessageConversationId, SendDirectMessageRequest)
    ): IO[SendMessageOutput] =
      val (conversationId, request) = input
      MessageCommands.sendMessage(connection, actor, conversationId, request, JdbcMessageRepository).flatMap {
        case sent @ SendMessageResult.Sent(message, recipientUsername) =>
          val event = MessageStreamEvent.MessageReceived(message)
          messageEventHub.publish(recipientUsername, event).as(
            SendMessageOutput(sent, Some(recipientUsername -> event))
          )
        case other =>
          IO.pure(SendMessageOutput(other, None))
      }

  final case class MarkConversationReadOutput(result: MarkConversationReadResult)

  final class MarkConversationReadPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[(MessageConversationId, MarkConversationReadRequest), MarkConversationReadOutput]:
    override val name: String = "MarkConversationRead"
    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (MessageConversationId, MarkConversationReadRequest)
    ): IO[MarkConversationReadOutput] =
      val (conversationId, request) = input
      MessageCommands.markConversationRead(connection, actor, conversationId, request, JdbcMessageRepository).flatMap {
        case marked @ MarkConversationReadResult.Marked(_, otherParticipant, Some(readUpToMessageId)) =>
          val event = MessageStreamEvent.ConversationRead(conversationId, readUpToMessageId, actor.username)
          messageEventHub.publish(otherParticipant, event) *> messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged).as(
            MarkConversationReadOutput(marked)
          )
        case other =>
          IO.pure(MarkConversationReadOutput(other))
      }

  final class MarkAllMessagesReadPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[Unit, MarkAllMessagesReadResult]:
    override val name: String = "MarkAllMessagesRead"
    override def execute(connection: Connection, actor: AuthUser, input: Unit): IO[MarkAllMessagesReadResult] =
      val _ = input
      MessageCommands.markAllMessagesRead(connection, actor, JdbcMessageRepository).flatTap { result =>
        val publishReceipts = result.receipts.traverse_(receipt =>
          messageEventHub.publish(
            receipt.otherParticipant,
            MessageStreamEvent.ConversationRead(receipt.conversationId, receipt.readUpToMessageId, actor.username)
          )
        )
        publishReceipts *> messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      }

  case object ListBlocks extends PlainAuthenticatedHttpPlan[Unit, List[domains.message.model.MessageBlockEntry]]:
    override val name: String = "ListBlocks"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[List[domains.message.model.MessageBlockEntry]] =
      val _ = input
      MessageCommands.listBlocks(databaseSession, actor, JdbcMessageRepository)

  final class AddBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[Username, AddBlockResult]:
    override val name: String = "AddBlock"
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[AddBlockResult] =
      MessageCommands.addBlock(connection, actor, input, JdbcMessageRepository).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )

  final class RemoveBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[Username, RemoveBlockResult]:
    override val name: String = "RemoveBlock"
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[RemoveBlockResult] =
      MessageCommands.removeBlock(connection, actor, input, JdbcMessageRepository).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )
