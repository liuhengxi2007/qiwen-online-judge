package domains.message.application



import cats.effect.IO
import domains.auth.model.AuthUser
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, MarkAllMessagesReadResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.application.input.{CreateConversationRequest, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.message.application.input.MarkConversationReadMode
import domains.message.model.MessageConversationId

import java.sql.Connection

object MessageMutationCommands:
  private val defaultRepository: MessageRepository = JdbcMessageRepository

  def createConversation(
    connection: Connection,
    actor: AuthUser,
    request: CreateConversationRequest,
    repository: MessageRepository = defaultRepository
  ): IO[CreateConversationResult] =
    if actor.username == request.targetUsername then IO.pure(CreateConversationResult.CannotMessageSelf)
    else
      repository.userExists(connection, request.targetUsername).flatMap {
        case false => IO.pure(CreateConversationResult.TargetUserNotFound)
        case true =>
          repository
            .getOrCreateConversation(connection, actor.username, request.targetUsername)
            .map(CreateConversationResult.Ready(_))
      }

  def sendMessage(
    connection: Connection,
    actor: AuthUser,
    conversationId: MessageConversationId,
    request: SendDirectMessageRequest,
    repository: MessageRepository = defaultRepository
  ): IO[SendMessageResult] =
    repository.findOtherParticipant(connection, actor.username, conversationId).flatMap {
      case None =>
        IO.pure(SendMessageResult.ConversationNotFound)
      case Some(recipientUsername) =>
        repository.isBlocked(connection, recipientUsername, actor.username).flatMap {
          case true => IO.pure(SendMessageResult.BlockedByRecipient)
          case false =>
            repository
              .insertMessage(connection, conversationId, actor.username, recipientUsername, request.content)
              .map(message => SendMessageResult.Sent(message, recipientUsername))
        }
    }

  def markConversationRead(
    connection: Connection,
    actor: AuthUser,
    conversationId: MessageConversationId,
    request: MarkConversationReadRequest,
    repository: MessageRepository = defaultRepository
  ): IO[MarkConversationReadResult] =
    repository.findConversationSummaryForUser(connection, actor.username, conversationId).flatMap {
      case None => IO.pure(MarkConversationReadResult.ConversationNotFound)
      case Some(existingSummary) =>
        repository.findOtherParticipant(connection, actor.username, conversationId).flatMap {
          case None =>
            IO.pure(MarkConversationReadResult.ConversationNotFound)
          case Some(otherParticipant) =>
            val markRead = request.mode match
              case MarkConversationReadMode.Message =>
                repository.markMessageRead(connection, conversationId, actor.username, request.messageId.get).map {
                  case true => request.messageId
                  case false => None
                }
              case MarkConversationReadMode.Conversation =>
                repository.markConversationRead(connection, conversationId, actor.username)

            markRead.flatMap { readUpToMessageId =>
              repository.findConversationSummaryForUser(connection, actor.username, conversationId).map { updatedSummary =>
                MarkConversationReadResult.Marked(updatedSummary.getOrElse(existingSummary), otherParticipant, readUpToMessageId)
              }
            }
        }
    }

  def markAllMessagesRead(
    connection: Connection,
    actor: AuthUser,
    repository: MessageRepository = defaultRepository
  ): IO[MarkAllMessagesReadResult] =
    repository.listUnreadConversationReadReceipts(connection, actor.username).flatMap { receipts =>
      repository.markAllMessagesRead(connection, actor.username).as(MarkAllMessagesReadResult(receipts))
    }

  def addBlock(
    connection: Connection,
    actor: AuthUser,
    targetUsername: domains.user.model.Username,
    repository: MessageRepository = defaultRepository
  ): IO[AddBlockResult] =
    if actor.username == targetUsername then IO.pure(AddBlockResult.CannotBlockSelf)
    else
      repository.userExists(connection, targetUsername).flatMap {
        case false => IO.pure(AddBlockResult.TargetUserNotFound)
        case true => repository.upsertBlock(connection, actor.username, targetUsername).map(AddBlockResult.Added(_))
      }

  def removeBlock(
    connection: Connection,
    actor: AuthUser,
    targetUsername: domains.user.model.Username,
    repository: MessageRepository = defaultRepository
  ): IO[RemoveBlockResult] =
    repository.removeBlock(connection, actor.username, targetUsername).as(RemoveBlockResult.Removed)
