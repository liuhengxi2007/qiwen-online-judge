package domains.message.application



import cats.effect.IO
import domains.auth.model.Username
import domains.message.http.response.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.shared.model.PageRequest

import java.sql.Connection

trait MessageRepository:
  def userExists(connection: Connection, username: Username): IO[Boolean]

  def getOrCreateConversation(
    connection: Connection,
    actorUsername: Username,
    targetUsername: Username
  ): IO[MessageConversationSummary]

  def findConversationSummaryForUser(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[MessageConversationSummary]]

  def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse]

  def findOtherParticipant(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[Username]]

  def listConversationMessages(
    connection: Connection,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Int
  ): IO[(List[DirectMessage], Boolean)]

  def getConversationMessageFacts(
    connection: Connection,
    conversationId: MessageConversationId,
    actorUsername: Username
  ): IO[ConversationMessageFacts]

  def insertMessage(
    connection: Connection,
    conversationId: MessageConversationId,
    senderUsername: Username,
    recipientUsername: Username,
    content: MessageContent
  ): IO[DirectMessage]

  def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean]

  def markConversationRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username
  ): IO[Option[MessageId]]

  def markMessageRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username,
    messageId: MessageId
  ): IO[Boolean]

  def markAllMessagesRead(
    connection: Connection,
    recipientUsername: Username
  ): IO[Unit]

  def listUnreadConversationReadReceipts(
    connection: Connection,
    recipientUsername: Username
  ): IO[List[ConversationReadReceipt]]

  def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]]

  def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry]

  def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit]
