package domains.message.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.application.{MessageCommands, MessageEventHub, MessageStreamEvent}
import domains.message.model.{CreateConversationRequest, MarkConversationReadRequest, MessageConversationId, MessageId, SendDirectMessageRequest}
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}

import java.sql.Connection

object MessageHttpPlans:

  case object ListInbox extends PlainAuthenticatedHttpPlan[Unit, domains.message.model.MessageInboxResponse]:
    override val name: String = "ListInbox"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[domains.message.model.MessageInboxResponse] =
      val _ = input
      MessageCommands.listInbox(databaseSession, actor)

  final case class HistoryInput(
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  )

  case object GetConversationHistory extends PlainAuthenticatedHttpPlan[HistoryInput, GetConversationHistoryResult]:
    override val name: String = "GetConversationHistory"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: HistoryInput): IO[GetConversationHistoryResult] =
      MessageCommands.getConversationHistory(databaseSession, actor, input.conversationId, input.beforeMessageId, input.limit)

  case object CreateConversation extends TransactionAuthenticatedHttpPlan[CreateConversationRequest, CreateConversationResult]:
    override val name: String = "CreateConversation"
    override def execute(connection: Connection, actor: AuthUser, input: CreateConversationRequest): IO[CreateConversationResult] =
      MessageCommands.createConversation(connection, actor, input)

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
      MessageCommands.sendMessage(connection, actor, conversationId, request).flatMap {
        case sent @ SendMessageResult.Sent(message, recipientUsername) =>
          val event = MessageStreamEvent.MessageReceived(message)
          messageEventHub.publish(recipientUsername, event).as(
            SendMessageOutput(sent, Some(recipientUsername -> event))
          )
        case other =>
          IO.pure(SendMessageOutput(other, None))
      }

  final case class MarkConversationReadOutput(
    result: MarkConversationReadResult,
    notification: Option[(Username, MessageStreamEvent.ConversationRead)]
  )

  final class MarkConversationReadPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[(MessageConversationId, MarkConversationReadRequest), MarkConversationReadOutput]:
    override val name: String = "MarkConversationRead"
    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (MessageConversationId, MarkConversationReadRequest)
    ): IO[MarkConversationReadOutput] =
      val (conversationId, request) = input
      MessageCommands.markConversationRead(connection, actor, conversationId, request).flatMap {
        case marked @ MarkConversationReadResult.Marked(_, otherParticipant, Some(readUpToMessageId)) =>
          val event = MessageStreamEvent.ConversationRead(conversationId, readUpToMessageId, actor.username)
          messageEventHub.publish(otherParticipant, event).as(
            MarkConversationReadOutput(marked, Some(otherParticipant -> event))
          )
        case other =>
          IO.pure(MarkConversationReadOutput(other, None))
      }

  case object ListBlocks extends PlainAuthenticatedHttpPlan[Unit, List[domains.message.model.MessageBlockEntry]]:
    override val name: String = "ListBlocks"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[List[domains.message.model.MessageBlockEntry]] =
      val _ = input
      MessageCommands.listBlocks(databaseSession, actor)

  final class AddBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[Username, AddBlockResult]:
    override val name: String = "AddBlock"
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[AddBlockResult] =
      MessageCommands.addBlock(connection, actor, input).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )

  final class RemoveBlockPlan(messageEventHub: MessageEventHub)
      extends TransactionAuthenticatedHttpPlan[Username, RemoveBlockResult]:
    override val name: String = "RemoveBlock"
    override def execute(connection: Connection, actor: AuthUser, input: Username): IO[RemoveBlockResult] =
      MessageCommands.removeBlock(connection, actor, input).flatTap(_ =>
        messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
      )
