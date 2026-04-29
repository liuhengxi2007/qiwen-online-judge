package domains.message.application

import cats.effect.IO
import domains.auth.model.Username
import domains.message.model.{DirectMessage, MessageBlockEntry, MessageContent, MessageConversationId, MessageConversationSummary, MessageId, MessageInboxResponse}
import domains.message.table.MessageTable

import java.sql.Connection

object JdbcMessageRepository extends MessageRepository:
  override def userExists(connection: Connection, username: Username): IO[Boolean] =
    MessageTable.userExists(connection, username)

  override def getOrCreateConversation(
    connection: Connection,
    actorUsername: Username,
    targetUsername: Username
  ): IO[MessageConversationSummary] =
    MessageTable.getOrCreateConversation(connection, actorUsername, targetUsername)

  override def findConversationSummaryForUser(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[MessageConversationSummary]] =
    MessageTable.findConversationSummaryForUser(connection, actorUsername, conversationId)

  override def listInbox(connection: Connection, actorUsername: Username): IO[MessageInboxResponse] =
    MessageTable.listInbox(connection, actorUsername)

  override def findOtherParticipant(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[Username]] =
    MessageTable.findOtherParticipant(connection, actorUsername, conversationId)

  override def listConversationMessages(
    connection: Connection,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Int
  ): IO[(List[DirectMessage], Boolean)] =
    MessageTable.listConversationMessages(connection, conversationId, beforeMessageId, limit)

  override def insertMessage(
    connection: Connection,
    conversationId: MessageConversationId,
    senderUsername: Username,
    recipientUsername: Username,
    content: MessageContent
  ): IO[DirectMessage] =
    MessageTable.insertMessage(connection, conversationId, senderUsername, recipientUsername, content)

  override def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
    MessageTable.isBlocked(connection, ownerUsername, blockedUsername)

  override def markConversationRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username
  ): IO[Option[MessageId]] =
    MessageTable.markConversationRead(connection, conversationId, recipientUsername)

  override def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
    MessageTable.listBlocks(connection, ownerUsername)

  override def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry] =
    MessageTable.upsertBlock(connection, ownerUsername, blockedUsername)

  override def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
    MessageTable.removeBlock(connection, ownerUsername, blockedUsername)
