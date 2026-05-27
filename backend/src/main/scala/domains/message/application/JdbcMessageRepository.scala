package domains.message.application



import cats.effect.IO
import domains.user.objects.Username
import domains.message.objects.response.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.objects.{MessageContent, MessageConversationId, MessageId}
import domains.message.objects.internal.ConversationReadReceipt
import shared.objects.PageRequest
import domains.message.table.message.{DirectMessageTable, MessageBlockTable, MessageConversationTable, MessageReadTable, MessageUserTable}

import java.sql.Connection

object JdbcMessageRepository extends MessageRepository:
  override def userExists(connection: Connection, username: Username): IO[Boolean] =
    MessageUserTable.userExists(connection, username)

  override def getOrCreateConversation(
    connection: Connection,
    actorUsername: Username,
    targetUsername: Username
  ): IO[MessageConversationSummary] =
    MessageConversationTable.getOrCreateConversation(connection, actorUsername, targetUsername)

  override def findConversationSummaryForUser(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[MessageConversationSummary]] =
    MessageConversationTable.findConversationSummaryForUser(connection, actorUsername, conversationId)

  override def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse] =
    MessageConversationTable.listInbox(connection, actorUsername, pageRequest)

  override def findOtherParticipant(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[Username]] =
    MessageConversationTable.findOtherParticipant(connection, actorUsername, conversationId)

  override def listConversationMessages(
    connection: Connection,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Int
  ): IO[(List[DirectMessage], Boolean)] =
    DirectMessageTable.listConversationMessages(connection, conversationId, beforeMessageId, limit)

  override def getConversationMessageFacts(
    connection: Connection,
    conversationId: MessageConversationId,
    actorUsername: Username
  ): IO[ConversationMessageFacts] =
    DirectMessageTable.getConversationMessageFacts(connection, conversationId, actorUsername)

  override def insertMessage(
    connection: Connection,
    conversationId: MessageConversationId,
    senderUsername: Username,
    recipientUsername: Username,
    content: MessageContent
  ): IO[DirectMessage] =
    DirectMessageTable.insertMessage(connection, conversationId, senderUsername, recipientUsername, content)

  override def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
    MessageBlockTable.isBlocked(connection, ownerUsername, blockedUsername)

  override def markConversationRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username
  ): IO[Option[MessageId]] =
    MessageReadTable.markConversationRead(connection, conversationId, recipientUsername)

  override def markMessageRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username,
    messageId: MessageId
  ): IO[Boolean] =
    MessageReadTable.markMessageRead(connection, conversationId, recipientUsername, messageId)

  override def markAllMessagesRead(
    connection: Connection,
    recipientUsername: Username
  ): IO[Unit] =
    MessageReadTable.markAllMessagesRead(connection, recipientUsername)

  override def listUnreadConversationReadReceipts(
    connection: Connection,
    recipientUsername: Username
  ): IO[List[ConversationReadReceipt]] =
    MessageReadTable.listUnreadConversationReadReceipts(connection, recipientUsername)

  override def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
    MessageBlockTable.listBlocks(connection, ownerUsername)

  override def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry] =
    MessageBlockTable.upsertBlock(connection, ownerUsername, blockedUsername)

  override def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
    MessageBlockTable.removeBlock(connection, ownerUsername, blockedUsername)
