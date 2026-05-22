package domains.message.table



import cats.effect.IO
import domains.auth.model.Username
import domains.message.http.response.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.message.table.MessageTableSchema.initialize
import domains.message.table.MessageTableSql.*
import domains.message.table.utils.MessageTableSupport.*
import domains.shared.model.PageRequest

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object MessageTable:

  def initialize(connection: Connection): IO[Unit] =
    MessageTableSchema.initialize(connection)

  def userExists(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(userExistsSql)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  def getOrCreateConversation(
    connection: Connection,
    actorUsername: Username,
    targetUsername: Username
  ): IO[MessageConversationSummary] =
    val (participantA, participantB) = normalizeConversationPair(actorUsername, targetUsername)
    findConversationIdForPair(connection, participantA, participantB).flatMap {
      case Some(conversationId) =>
        findConversationSummaryForUser(connection, actorUsername, conversationId).map(_.getOrElse {
          throw IllegalStateException("Conversation exists but is not readable by participant.")
        })
      case None =>
        for
          conversationId <- IO.delay(MessageConversationId(UUID.randomUUID()))
          now <- IO.realTimeInstant
          _ <- IO.blocking {
            val statement = connection.prepareStatement(insertConversationSql)
            try
              statement.setObject(1, conversationId.value)
              statement.setString(2, participantA.value)
              statement.setString(3, participantB.value)
              statement.setTimestamp(4, Timestamp.from(now))
              statement.setTimestamp(5, Timestamp.from(now))
              statement.setTimestamp(6, Timestamp.from(now))
              statement.executeUpdate()
              ()
            finally statement.close()
          }
          summary <- findConversationSummaryForUser(connection, actorUsername, conversationId).map(_.getOrElse {
            throw IllegalStateException("Created conversation is not readable by participant.")
          })
        yield summary
    }

  def findConversationSummaryForUser(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[MessageConversationSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(findConversationSummaryForUserSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setString(2, actorUsername.value)
        statement.setString(3, actorUsername.value)
        statement.setObject(4, conversationId.value)
        statement.setString(5, actorUsername.value)
        statement.setString(6, actorUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readConversationSummary(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse] =
    val normalizedPageRequest = pageRequest.normalized
    for
      conversations <- IO.blocking {
        val statement = connection.prepareStatement(listInboxSql)
        try
          statement.setString(1, actorUsername.value)
          statement.setString(2, actorUsername.value)
          statement.setString(3, actorUsername.value)
          statement.setString(4, actorUsername.value)
          statement.setString(5, actorUsername.value)
          statement.setInt(6, normalizedPageRequest.pageSize)
          statement.setInt(7, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readConversationSummary(resultSet)).toList
          finally resultSet.close()
        finally statement.close()
      }
      totalUnreadCount <- IO.blocking {
        val statement = connection.prepareStatement(listUnreadMessageCountsSql)
        try
          statement.setString(1, actorUsername.value)
          statement.setString(2, actorUsername.value)
          statement.setString(3, actorUsername.value)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getInt("total_unread_count") else 0
          finally resultSet.close()
        finally statement.close()
      }
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countInboxSql)
        try
          statement.setString(1, actorUsername.value)
          statement.setString(2, actorUsername.value)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
    yield MessageInboxResponse(conversations, totalUnreadCount, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  def findOtherParticipant(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[Username]] =
    IO.blocking {
      val statement = connection.prepareStatement(findOtherParticipantSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setObject(2, conversationId.value)
        statement.setString(3, actorUsername.value)
        statement.setString(4, actorUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(Username.canonical(resultSet.getString("other_username"))) else None
        finally resultSet.close()
      finally statement.close()
    }

  def listConversationMessages(
    connection: Connection,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Int
  ): IO[(List[DirectMessage], Boolean)] =
    IO.blocking {
      val queryLimit = Math.max(1, limit) + 1
      val statement = beforeMessageId match
        case Some(_) => connection.prepareStatement(listConversationMessagesBeforeSql)
        case None => connection.prepareStatement(listConversationMessagesSql)

      try
        statement.setObject(1, conversationId.value)
        beforeMessageId match
          case Some(messageId) =>
            statement.setObject(2, messageId.value)
            statement.setInt(3, queryLimit)
          case None =>
            statement.setInt(2, queryLimit)

        val resultSet = statement.executeQuery()
        try
          val descendingMessages =
            Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readDirectMessage(resultSet)).toList
          val hasMore = descendingMessages.size > limit
          val visibleMessages = descendingMessages.take(limit).reverse
          (visibleMessages, hasMore)
        finally resultSet.close()
      finally statement.close()
    }

  def getConversationMessageFacts(
    connection: Connection,
    conversationId: MessageConversationId,
    actorUsername: Username
  ): IO[ConversationMessageFacts] =
    IO.blocking {
      val statement = connection.prepareStatement(conversationMessageFactsSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setString(2, actorUsername.value)
        statement.setObject(3, conversationId.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            ConversationMessageFacts(
              viewerHasSentMessage = resultSet.getBoolean("viewer_has_sent_message"),
              otherParticipantMessageCount = resultSet.getInt("other_participant_message_count")
            )
          else ConversationMessageFacts(viewerHasSentMessage = false, otherParticipantMessageCount = 0)
        finally resultSet.close()
      finally statement.close()
    }

  def insertMessage(
    connection: Connection,
    conversationId: MessageConversationId,
    senderUsername: Username,
    recipientUsername: Username,
    content: MessageContent
  ): IO[DirectMessage] =
    for
      messageId <- IO.delay(MessageId(UUID.randomUUID()))
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(insertMessageSql)
        try
          statement.setObject(1, messageId.value)
          statement.setObject(2, conversationId.value)
          statement.setString(3, senderUsername.value)
          statement.setString(4, recipientUsername.value)
          statement.setString(5, content.value)
          statement.setTimestamp(6, Timestamp.from(now))
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      _ <- IO.blocking {
        val statement = connection.prepareStatement(touchConversationSql)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setTimestamp(2, Timestamp.from(now))
          statement.setObject(3, conversationId.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      message <- IO.blocking {
        val statement = connection.prepareStatement(readInsertedMessageSql)
        try
          statement.setObject(1, messageId.value)
          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readDirectMessage(resultSet)
            else throw IllegalStateException("Inserted message is missing.")
          finally resultSet.close()
        finally statement.close()
      }
    yield message

  def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(isBlockedSql)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  def markConversationRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username
  ): IO[Option[MessageId]] =
    for
      readUpToMessageId <- IO.blocking {
        val statement = connection.prepareStatement(findLastUnreadMessageInConversationSql)
        try
          statement.setObject(1, conversationId.value)
          statement.setString(2, recipientUsername.value)
          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then Some(MessageId(resultSet.getObject("id", classOf[java.util.UUID])))
            else None
          finally resultSet.close()
        finally statement.close()
      }
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(markConversationReadSql)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setObject(2, conversationId.value)
          statement.setString(3, recipientUsername.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
    yield readUpToMessageId

  def markMessageRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username,
    messageId: MessageId
  ): IO[Boolean] =
    for
      now <- IO.realTimeInstant
      updatedCount <- IO.blocking {
        val statement = connection.prepareStatement(markMessageReadSql)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setObject(2, messageId.value)
          statement.setObject(3, conversationId.value)
          statement.setString(4, recipientUsername.value)
          statement.executeUpdate()
        finally statement.close()
      }
    yield updatedCount > 0

  def markAllMessagesRead(
    connection: Connection,
    recipientUsername: Username
  ): IO[Unit] =
    for
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(markAllMessagesReadSql)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setString(2, recipientUsername.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
    yield ()

  def listUnreadConversationReadReceipts(
    connection: Connection,
    recipientUsername: Username
  ): IO[List[ConversationReadReceipt]] =
    IO.blocking {
      val statement = connection.prepareStatement(listUnreadConversationReadReceiptsSql)
      try
        statement.setString(1, recipientUsername.value)
        statement.setString(2, recipientUsername.value)
        statement.setString(3, recipientUsername.value)
        statement.setString(4, recipientUsername.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readConversationReadReceipt(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
    IO.blocking {
      val statement = connection.prepareStatement(listBlocksSql)
      try
        statement.setString(1, ownerUsername.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readBlockEntry(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry] =
    for
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(upsertBlockSql)
        try
          statement.setString(1, ownerUsername.value)
          statement.setString(2, blockedUsername.value)
          statement.setTimestamp(3, Timestamp.from(now))
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      entry <- IO.blocking {
        val statement = connection.prepareStatement(readBlockEntrySql)
        try
          statement.setString(1, ownerUsername.value)
          statement.setString(2, blockedUsername.value)
          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readBlockEntry(resultSet)
            else throw IllegalStateException("Inserted block entry is missing.")
          finally resultSet.close()
        finally statement.close()
      }
    yield entry

  def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(removeBlockSql)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def findConversationIdForPair(
    connection: Connection,
    participantA: Username,
    participantB: Username
  ): IO[Option[MessageConversationId]] =
    IO.blocking {
      val statement = connection.prepareStatement(findConversationForPairSql)
      try
        statement.setString(1, participantA.value)
        statement.setString(2, participantB.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(MessageConversationId(resultSet.getObject("id", classOf[java.util.UUID])))
          else None
        finally resultSet.close()
      finally statement.close()
    }
