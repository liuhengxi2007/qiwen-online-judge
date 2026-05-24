package domains.message.table.message



import cats.effect.IO
import domains.user.model.Username
import domains.message.application.output.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.message.table.message.MessageTableSchema.initialize
import domains.message.table.message.MessageTableSupport.*
import shared.model.PageRequest

import java.sql.{Connection, Timestamp}
import java.util.UUID

object MessageTable:

  def initialize(connection: Connection): IO[Unit] =
    MessageTableSchema.initialize(connection)

  private val userExistsSQL: String =
    """
      |select 1
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  def userExists(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(userExistsSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val insertConversationSQL: String =
    """
      |insert into message_conversations (
      |  id,
      |  participant_a_username,
      |  participant_b_username,
      |  created_at,
      |  updated_at,
      |  last_message_at
      |)
      |values (?, ?, ?, ?, ?, ?)
      |""".stripMargin

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
            val statement = connection.prepareStatement(insertConversationSQL)
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

  private val findConversationSummaryForUserSQL: String =
    """
      |select mc.id,
      |       case
      |         when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |         else mc.participant_a_username
      |       end as other_username,
      |       au.display_name as other_display_name,
      |       lm.sender_username as last_message_sender_username,
      |       case
      |         when lm.content is null then null
      |         when length(lm.content) <= 140 then lm.content
      |         else left(lm.content, 140)
      |       end as last_message_preview,
      |       coalesce(lm.created_at, mc.last_message_at) as last_message_at,
      |       coalesce(unread.unread_count, 0) as unread_count
      |from message_conversations mc
      |join auth_users au on lower(au.username) = lower(
      |  case
      |    when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |    else mc.participant_a_username
      |  end
      |)
      |left join lateral (
      |  select sender_username, content, created_at
      |  from direct_messages
      |  where conversation_id = mc.id
      |  order by created_at desc, id desc
      |  limit 1
      |) lm on true
      |left join lateral (
      |  select count(*)::int as unread_count
      |  from direct_messages
      |  where conversation_id = mc.id
      |    and lower(recipient_username) = lower(?)
      |    and read_at is null
      |) unread on true
      |where mc.id = ?
      |  and (lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?))
      |""".stripMargin

  def findConversationSummaryForUser(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[MessageConversationSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(findConversationSummaryForUserSQL)
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

  private val listInboxSQL: String =
    """
      |select mc.id,
      |       case
      |         when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |         else mc.participant_a_username
      |       end as other_username,
      |       au.display_name as other_display_name,
      |       lm.sender_username as last_message_sender_username,
      |       case
      |         when lm.content is null then null
      |         when length(lm.content) <= 140 then lm.content
      |         else left(lm.content, 140)
      |       end as last_message_preview,
      |       coalesce(lm.created_at, mc.last_message_at) as last_message_at,
      |       coalesce(unread.unread_count, 0) as unread_count
      |from message_conversations mc
      |join auth_users au on lower(au.username) = lower(
      |  case
      |    when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |    else mc.participant_a_username
      |  end
      |)
      |left join lateral (
      |  select sender_username, content, created_at
      |  from direct_messages
      |  where conversation_id = mc.id
      |  order by created_at desc, id desc
      |  limit 1
      |) lm on true
      |left join lateral (
      |  select count(*)::int as unread_count
      |  from direct_messages
      |  where conversation_id = mc.id
      |    and lower(recipient_username) = lower(?)
      |    and read_at is null
      |) unread on true
      |where lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?)
      |order by mc.last_message_at desc, mc.created_at desc, mc.id desc
      |limit ? offset ?
      |""".stripMargin

  private val countInboxSQL: String =
    """
      |select count(*) as total_items
      |from message_conversations mc
      |where lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?)
      |""".stripMargin

  private val listUnreadMessageCountsSQL: String =
    """
      |select count(*)::int as total_unread_count
      |from direct_messages dm
      |join message_conversations mc on mc.id = dm.conversation_id
      |where lower(dm.recipient_username) = lower(?)
      |  and dm.read_at is null
      |  and (lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?))
      |""".stripMargin

  def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse] =
    val normalizedPageRequest = pageRequest.normalized
    for
      conversations <- IO.blocking {
        val statement = connection.prepareStatement(listInboxSQL)
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
        val statement = connection.prepareStatement(listUnreadMessageCountsSQL)
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
        val statement = connection.prepareStatement(countInboxSQL)
        try
          statement.setString(1, actorUsername.value)
          statement.setString(2, actorUsername.value)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
    yield MessageInboxResponse(conversations, totalUnreadCount, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val findOtherParticipantSQL: String =
    """
      |select case
      |         when lower(participant_a_username) = lower(?) then participant_b_username
      |         else participant_a_username
      |       end as other_username
      |from message_conversations
      |where id = ?
      |  and (lower(participant_a_username) = lower(?) or lower(participant_b_username) = lower(?))
      |""".stripMargin

  def findOtherParticipant(
    connection: Connection,
    actorUsername: Username,
    conversationId: MessageConversationId
  ): IO[Option[Username]] =
    IO.blocking {
      val statement = connection.prepareStatement(findOtherParticipantSQL)
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

  private val listConversationMessagesSQL: String =
    """
      |select dm.id,
      |       dm.conversation_id,
      |       dm.sender_username,
      |       sender.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join auth_users sender on lower(sender.username) = lower(dm.sender_username)
      |where dm.conversation_id = ?
      |order by dm.created_at desc, dm.id desc
      |limit ?
      |""".stripMargin

  private val listConversationMessagesBeforeSQL: String =
    """
      |select dm.id,
      |       dm.conversation_id,
      |       dm.sender_username,
      |       sender.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join auth_users sender on lower(sender.username) = lower(dm.sender_username)
      |where dm.conversation_id = ?
      |  and (dm.created_at, dm.id) < (
      |    select created_at, id
      |    from direct_messages
      |    where id = ?
      |  )
      |order by dm.created_at desc, dm.id desc
      |limit ?
      |""".stripMargin

  def listConversationMessages(
    connection: Connection,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Int
  ): IO[(List[DirectMessage], Boolean)] =
    IO.blocking {
      val queryLimit = Math.max(1, limit) + 1
      val statement = beforeMessageId match
        case Some(_) => connection.prepareStatement(listConversationMessagesBeforeSQL)
        case None => connection.prepareStatement(listConversationMessagesSQL)

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

  private val conversationMessageFactsSQL: String =
    """
      |select coalesce(bool_or(lower(sender_username) = lower(?)), false) as viewer_has_sent_message,
      |       count(*) filter (where lower(sender_username) <> lower(?))::int as other_participant_message_count
      |from direct_messages
      |where conversation_id = ?
      |""".stripMargin

  def getConversationMessageFacts(
    connection: Connection,
    conversationId: MessageConversationId,
    actorUsername: Username
  ): IO[ConversationMessageFacts] =
    IO.blocking {
      val statement = connection.prepareStatement(conversationMessageFactsSQL)
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

  private val insertMessageSQL: String =
    """
      |insert into direct_messages (
      |  id,
      |  conversation_id,
      |  sender_username,
      |  recipient_username,
      |  content,
      |  created_at,
      |  read_at
      |)
      |values (?, ?, ?, ?, ?, ?, null)
      |""".stripMargin

  private val touchConversationSQL: String =
    """
      |update message_conversations
      |set updated_at = ?, last_message_at = ?
      |where id = ?
      |""".stripMargin

  private val readInsertedMessageSQL: String =
    """
      |select dm.id,
      |       dm.conversation_id,
      |       dm.sender_username,
      |       sender.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join auth_users sender on lower(sender.username) = lower(dm.sender_username)
      |where dm.id = ?
      |""".stripMargin

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
        val statement = connection.prepareStatement(insertMessageSQL)
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
        val statement = connection.prepareStatement(touchConversationSQL)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setTimestamp(2, Timestamp.from(now))
          statement.setObject(3, conversationId.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      message <- IO.blocking {
        val statement = connection.prepareStatement(readInsertedMessageSQL)
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

  private val isBlockedSQL: String =
    """
      |select 1
      |from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

  def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(isBlockedSQL)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val markConversationReadSQL: String =
    """
      |update direct_messages
      |set read_at = ?
      |where conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  private val findLastUnreadMessageInConversationSQL: String =
    """
      |select id
      |from direct_messages
      |where conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |order by created_at desc, id desc
      |limit 1
      |""".stripMargin

  def markConversationRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username
  ): IO[Option[MessageId]] =
    for
      readUpToMessageId <- IO.blocking {
        val statement = connection.prepareStatement(findLastUnreadMessageInConversationSQL)
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
        val statement = connection.prepareStatement(markConversationReadSQL)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setObject(2, conversationId.value)
          statement.setString(3, recipientUsername.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
    yield readUpToMessageId

  private val markMessageReadSQL: String =
    """
      |update direct_messages
      |set read_at = ?
      |where id = ?
      |  and conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  def markMessageRead(
    connection: Connection,
    conversationId: MessageConversationId,
    recipientUsername: Username,
    messageId: MessageId
  ): IO[Boolean] =
    for
      now <- IO.realTimeInstant
      updatedCount <- IO.blocking {
        val statement = connection.prepareStatement(markMessageReadSQL)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setObject(2, messageId.value)
          statement.setObject(3, conversationId.value)
          statement.setString(4, recipientUsername.value)
          statement.executeUpdate()
        finally statement.close()
      }
    yield updatedCount > 0

  private val markAllMessagesReadSQL: String =
    """
      |update direct_messages
      |set read_at = ?
      |where lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  def markAllMessagesRead(
    connection: Connection,
    recipientUsername: Username
  ): IO[Unit] =
    for
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(markAllMessagesReadSQL)
        try
          statement.setTimestamp(1, Timestamp.from(now))
          statement.setString(2, recipientUsername.value)
          statement.executeUpdate()
          ()
        finally statement.close()
      }
    yield ()

  private val listUnreadConversationReadReceiptsSQL: String =
    """
      |select mc.id as conversation_id,
      |       case
      |         when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |         else mc.participant_a_username
      |       end as other_username,
      |       unread.id as read_up_to_message_id
      |from message_conversations mc
      |join lateral (
      |  select dm.id
      |  from direct_messages dm
      |  where dm.conversation_id = mc.id
      |    and lower(dm.recipient_username) = lower(?)
      |    and dm.read_at is null
      |  order by dm.created_at desc, dm.id desc
      |  limit 1
      |) unread on true
      |where lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?)
      |""".stripMargin

  def listUnreadConversationReadReceipts(
    connection: Connection,
    recipientUsername: Username
  ): IO[List[ConversationReadReceipt]] =
    IO.blocking {
      val statement = connection.prepareStatement(listUnreadConversationReadReceiptsSQL)
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

  private val listBlocksSQL: String =
    """
      |select mb.blocked_username,
      |       au.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join auth_users au on lower(au.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |order by mb.created_at desc, lower(mb.blocked_username) asc
      |""".stripMargin

  def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
    IO.blocking {
      val statement = connection.prepareStatement(listBlocksSQL)
      try
        statement.setString(1, ownerUsername.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readBlockEntry(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  private val upsertBlockSQL: String =
    """
      |insert into message_blocks (owner_username, blocked_username, created_at)
      |values (?, ?, ?)
      |on conflict (owner_username, blocked_username)
      |do update set created_at = excluded.created_at
      |""".stripMargin

  private val readBlockEntrySQL: String =
    """
      |select mb.blocked_username,
      |       au.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join auth_users au on lower(au.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |  and lower(mb.blocked_username) = lower(?)
      |""".stripMargin

  def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry] =
    for
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(upsertBlockSQL)
        try
          statement.setString(1, ownerUsername.value)
          statement.setString(2, blockedUsername.value)
          statement.setTimestamp(3, Timestamp.from(now))
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      entry <- IO.blocking {
        val statement = connection.prepareStatement(readBlockEntrySQL)
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

  private val removeBlockSQL: String =
    """
      |delete from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

  def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(removeBlockSQL)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val findConversationForPairSQL: String =
    """
      |select id
      |from message_conversations
      |where lower(participant_a_username) = lower(?)
      |  and lower(participant_b_username) = lower(?)
      |""".stripMargin

  private def findConversationIdForPair(
    connection: Connection,
    participantA: Username,
    participantB: Username
  ): IO[Option[MessageConversationId]] =
    IO.blocking {
      val statement = connection.prepareStatement(findConversationForPairSQL)
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
