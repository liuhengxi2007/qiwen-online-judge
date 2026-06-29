package domains.message.table.message

import cats.effect.IO
import domains.message.objects.MessageConversationId
import domains.message.objects.response.{MessageConversationSummary, MessageInboxResponse}
import domains.message.table.message.MessageTableSupport.*
import domains.user.objects.Username
import shared.objects.PageRequest

import java.sql.{Connection, SQLException, Timestamp}
import java.util.UUID

/** 私信会话表访问对象，负责会话创建、收件箱列表和参与者校验。 */
object MessageConversationTable:

  /** 获取两名用户之间的会话；不存在时按用户名规范顺序创建并返回当前用户视角摘要。 */
  def getOrCreateConversation(
    connection: Connection,
    actorUsername: Username,
    targetUsername: Username
  ): IO[MessageConversationSummary] =
    val (participantA, participantB) = normalizeConversationPair(actorUsername, targetUsername)
    findConversationIdForPair(connection, participantA, participantB).flatMap {
      case Some(conversationId) =>
        /** 注意：会话 id 来自同一参与者对查询；随后按 actor 读取不到摘要表示表内参与者不变量被破坏。 */
        findConversationSummaryForUser(connection, actorUsername, conversationId).map(_.getOrElse {
          throw IllegalStateException("Conversation exists but is not readable by participant.")
        })
      case None =>
        for
          conversationId <- IO.delay(MessageConversationId(UUID.randomUUID()))
          now <- IO.realTimeInstant
          createdOrExistingId <- insertConversation(connection, conversationId, participantA, participantB, now).handleErrorWith {
            case error: SQLException if error.getSQLState == "23505" =>
              findConversationIdForPair(connection, participantA, participantB).flatMap {
                case Some(existingId) => IO.pure(existingId)
                case None => IO.raiseError(error)
              }
            case error => IO.raiseError(error)
          }
          summary <- findConversationSummaryForUser(connection, actorUsername, createdOrExistingId).map(_.getOrElse {
            /** 注意：会话刚按 actor/target 创建；创建后不可读表示写入或读取 SQL 的参与者不变量被破坏。 */
            throw IllegalStateException("Created conversation is not readable by participant.")
          })
        yield summary
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

  private def insertConversation(
    connection: Connection,
    conversationId: MessageConversationId,
    participantA: Username,
    participantB: Username,
    now: java.time.Instant
  ): IO[MessageConversationId] =
    IO.blocking {
      val statement = connection.prepareStatement(insertConversationSQL)
      try
        statement.setObject(1, conversationId.value)
        statement.setString(2, participantA.value)
        statement.setString(3, participantB.value)
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setTimestamp(5, Timestamp.from(now))
        statement.setTimestamp(6, Timestamp.from(now))
        statement.executeUpdate()
        conversationId
      finally statement.close()
    }

  private val findConversationSummaryForUserSQL: String =
    """
      |select mc.id,
      |       case
      |         when lower(mc.participant_a_username) = lower(?) then mc.participant_b_username
      |         else mc.participant_a_username
      |       end as other_username,
      |       up.display_name as other_display_name,
      |       lm.sender_username as last_message_sender_username,
      |       case
      |         when lm.content is null then null
      |         when length(lm.content) <= 140 then lm.content
      |         else left(lm.content, 140)
      |       end as last_message_preview,
      |       coalesce(lm.created_at, mc.last_message_at) as last_message_at,
      |       coalesce(unread.unread_count, 0) as unread_count
      |from message_conversations mc
      |join user_profiles up on lower(up.username) = lower(
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

  /** 按会话 id 读取当前用户视角的会话摘要，非参与者返回 None。 */
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
      |       up.display_name as other_display_name,
      |       lm.sender_username as last_message_sender_username,
      |       case
      |         when lm.content is null then null
      |         when length(lm.content) <= 140 then lm.content
      |         else left(lm.content, 140)
      |       end as last_message_preview,
      |       coalesce(lm.created_at, mc.last_message_at) as last_message_at,
      |       coalesce(unread.unread_count, 0) as unread_count
      |from message_conversations mc
      |join user_profiles up on lower(up.username) = lower(
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

  /** 分页读取当前用户参与的所有会话，并统计总未读消息数。 */
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

  /** 在确认当前用户属于会话的同时读取另一名参与者用户名。 */
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
