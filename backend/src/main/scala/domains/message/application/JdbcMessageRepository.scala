package domains.message.application



import cats.effect.IO
import domains.auth.model.Username
import domains.message.application.view.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.shared.model.PageRequest
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

  override def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse] =
    MessageTable.listInbox(connection, actorUsername, pageRequest)

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

  override def getConversationMessageFacts(
    connection: Connection,
    conversationId: MessageConversationId,
    actorUsername: Username
  ): IO[ConversationMessageFacts] =
    MessageTable.getConversationMessageFacts(connection, conversationId, actorUsername)

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

  override def markMessageRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username,
    messageId: MessageId
  ): IO[Boolean] =
    MessageTable.markMessageRead(connection, conversationId, recipientUsername, messageId)

  override def markAllMessagesRead(
    connection: Connection,
    recipientUsername: Username
  ): IO[Unit] =
    MessageTable.markAllMessagesRead(connection, recipientUsername)

  override def listUnreadConversationReadReceipts(
    connection: Connection,
    recipientUsername: Username
  ): IO[List[ConversationReadReceipt]] =
    MessageTable.listUnreadConversationReadReceipts(connection, recipientUsername)

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
