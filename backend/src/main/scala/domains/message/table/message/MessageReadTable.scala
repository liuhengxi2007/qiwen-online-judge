package domains.message.table.message

import cats.effect.IO
import domains.message.objects.{MessageConversationId, MessageId}
import domains.message.objects.internal.ConversationReadReceipt
import domains.message.table.message.MessageTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}

object MessageReadTable:

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
