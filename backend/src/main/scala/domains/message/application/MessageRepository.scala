package domains.message.application

import cats.effect.IO
import domains.auth.model.Username
import domains.message.model.{DirectMessage, MessageBlockEntry, MessageContent, MessageConversationId, MessageConversationSummary, MessageId, MessageInboxResponse}

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

  def listInbox(connection: Connection, actorUsername: Username): IO[MessageInboxResponse]

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

  def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]]

  def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry]

  def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit]
