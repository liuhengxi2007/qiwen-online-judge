package domains.message.table.message

import cats.effect.IO
import domains.message.objects.response.{ConversationMessageFacts, DirectMessage}
import domains.message.objects.{MessageContent, MessageConversationId, MessageId}
import domains.message.table.message.MessageTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.util.UUID

object DirectMessageTable:

  private val listConversationMessagesSQL: String =
    """
      |select dm.id,
      |       dm.conversation_id,
      |       dm.sender_username,
      |       sender_profile.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join user_profiles sender_profile on lower(sender_profile.username) = lower(dm.sender_username)
      |where dm.conversation_id = ?
      |order by dm.created_at desc, dm.id desc
      |limit ?
      |""".stripMargin

  private val listConversationMessagesBeforeSQL: String =
    """
      |select dm.id,
      |       dm.conversation_id,
      |       dm.sender_username,
      |       sender_profile.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join user_profiles sender_profile on lower(sender_profile.username) = lower(dm.sender_username)
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
      |       sender_profile.display_name as sender_display_name,
      |       dm.recipient_username,
      |       dm.content,
      |       dm.created_at,
      |       dm.read_at
      |from direct_messages dm
      |join user_profiles sender_profile on lower(sender_profile.username) = lower(dm.sender_username)
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
