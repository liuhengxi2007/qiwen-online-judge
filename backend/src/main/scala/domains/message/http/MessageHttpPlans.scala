package domains.message.http



import cats.syntax.all.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.user.objects.Username
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, MarkAllMessagesReadResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.application.{JdbcMessageRepository, MessageCommands, MessageEventHub, MessageStreamEvent}
import domains.message.objects.request.{CreateConversationRequest, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.message.objects.{MessageConversationId, MessageId}
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import shared.objects.PageRequest

import java.sql.Connection

object MessageHttpPlans:

  case object ListInbox extends PlainAuthenticatedHttpPlan[AuthUser, PageRequest, domains.message.objects.response.MessageInboxResponse]:
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: PageRequest): IO[domains.message.objects.response.MessageInboxResponse] =
      MessageCommands.listInbox(databaseSession, actor, input, JdbcMessageRepository)

  final case class HistoryInput(
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  )

  case object GetConversationHistory extends PlainAuthenticatedHttpPlan[AuthUser, HistoryInput, GetConversationHistoryResult]:
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: HistoryInput): IO[GetConversationHistoryResult] =
      MessageCommands.getConversationHistory(databaseSession, actor, input.conversationId, input.beforeMessageId, input.limit, JdbcMessageRepository)

  case object CreateConversation extends TransactionAuthenticatedHttpPlan[AuthUser, CreateConversationRequest, CreateConversationResult]:
    override def execute(connection: Connection, actor: AuthUser, input: CreateConversationRequest): IO[CreateConversationResult] =
      MessageCommands.createConversation(connection, actor, input, JdbcMessageRepository)

  final case class SendMessageOutput(
    result: SendMessageResult,
    notification: Option[(Username, MessageStreamEvent.MessageReceived)]
  )

  final class SendMessagePlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, (MessageConversationId, SendDirectMessageRequest), SendMessageOutput]:
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
      extends TransactionAuthenticatedHttpPlan[AuthUser, (MessageConversationId, MarkConversationReadRequest), MarkConversationReadOutput]:
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
      extends TransactionAuthenticatedHttpPlan[AuthUser, Unit, MarkAllMessagesReadResult]:
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

  case object ListBlocks extends PlainAuthenticatedHttpPlan[AuthUser, Unit, List[domains.message.objects.response.MessageBlockEntry]]:
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[List[domains.message.objects.response.MessageBlockEntry]] =
      val _ = input
      MessageCommands.listBlocks(databaseSession, actor, JdbcMessageRepository)

  final class AddBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, Username, AddBlockResult]:
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[AddBlockResult] =
      MessageCommands.addBlock(connection, actor, input, JdbcMessageRepository).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )

  final class RemoveBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, Username, RemoveBlockResult]:
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[RemoveBlockResult] =
      MessageCommands.removeBlock(connection, actor, input, JdbcMessageRepository).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )
