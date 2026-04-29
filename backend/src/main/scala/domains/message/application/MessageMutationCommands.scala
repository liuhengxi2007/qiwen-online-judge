package domains.message.application

import cats.effect.IO
import domains.auth.model.AuthUser
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.model.{CreateConversationRequest, MarkConversationReadRequest, MessageConversationId, SendDirectMessageRequest}

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
    val _ = request
    repository.findConversationSummaryForUser(connection, actor.username, conversationId).flatMap {
      case None => IO.pure(MarkConversationReadResult.ConversationNotFound)
      case Some(summary) =>
        repository.findOtherParticipant(connection, actor.username, conversationId).flatMap {
          case None =>
            IO.pure(MarkConversationReadResult.ConversationNotFound)
          case Some(otherParticipant) =>
            repository.markConversationRead(connection, conversationId, actor.username).map { readUpToMessageId =>
              MarkConversationReadResult.Marked(summary, otherParticipant, readUpToMessageId)
            }
        }
    }

  def addBlock(
    connection: Connection,
    actor: AuthUser,
    targetUsername: domains.auth.model.Username,
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
    targetUsername: domains.auth.model.Username,
    repository: MessageRepository = defaultRepository
  ): IO[RemoveBlockResult] =
    repository.removeBlock(connection, actor.username, targetUsername).as(RemoveBlockResult.Removed)
