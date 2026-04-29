package domains.message.table

object MessageTableSql:
  val userExistsSql: String =
    """
      |select 1
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  val findConversationForPairSql: String =
    """
      |select id
      |from message_conversations
      |where lower(participant_a_username) = lower(?)
      |  and lower(participant_b_username) = lower(?)
      |""".stripMargin

  val insertConversationSql: String =
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

  val findConversationSummaryForUserSql: String =
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

  val listInboxSql: String =
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
      |""".stripMargin

  val listUnreadMessageCountsSql: String =
    """
      |select count(*)::int as total_unread_count
      |from direct_messages dm
      |join message_conversations mc on mc.id = dm.conversation_id
      |where lower(dm.recipient_username) = lower(?)
      |  and dm.read_at is null
      |  and (lower(mc.participant_a_username) = lower(?) or lower(mc.participant_b_username) = lower(?))
      |""".stripMargin

  val findOtherParticipantSql: String =
    """
      |select case
      |         when lower(participant_a_username) = lower(?) then participant_b_username
      |         else participant_a_username
      |       end as other_username
      |from message_conversations
      |where id = ?
      |  and (lower(participant_a_username) = lower(?) or lower(participant_b_username) = lower(?))
      |""".stripMargin

  val listConversationMessagesSql: String =
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

  val listConversationMessagesBeforeSql: String =
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

  val conversationMessageFactsSql: String =
    """
      |select coalesce(bool_or(lower(sender_username) = lower(?)), false) as viewer_has_sent_message,
      |       count(*) filter (where lower(sender_username) <> lower(?))::int as other_participant_message_count
      |from direct_messages
      |where conversation_id = ?
      |""".stripMargin

  val insertMessageSql: String =
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

  val touchConversationSql: String =
    """
      |update message_conversations
      |set updated_at = ?, last_message_at = ?
      |where id = ?
      |""".stripMargin

  val readInsertedMessageSql: String =
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

  val isBlockedSql: String =
    """
      |select 1
      |from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

  val markConversationReadSql: String =
    """
      |update direct_messages
      |set read_at = ?
      |where conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  val findLastUnreadMessageInConversationSql: String =
    """
      |select id
      |from direct_messages
      |where conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |order by created_at desc, id desc
      |limit 1
      |""".stripMargin

  val markMessageReadSql: String =
    """
      |update direct_messages
      |set read_at = ?
      |where id = ?
      |  and conversation_id = ?
      |  and lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  val markAllMessagesReadSql: String =
    """
      |update direct_messages
      |set read_at = ?
      |where lower(recipient_username) = lower(?)
      |  and read_at is null
      |""".stripMargin

  val listUnreadConversationReadReceiptsSql: String =
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

  val listBlocksSql: String =
    """
      |select mb.blocked_username,
      |       au.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join auth_users au on lower(au.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |order by mb.created_at desc, lower(mb.blocked_username) asc
      |""".stripMargin

  val upsertBlockSql: String =
    """
      |insert into message_blocks (owner_username, blocked_username, created_at)
      |values (?, ?, ?)
      |on conflict (owner_username, blocked_username)
      |do update set created_at = excluded.created_at
      |""".stripMargin

  val readBlockEntrySql: String =
    """
      |select mb.blocked_username,
      |       au.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join auth_users au on lower(au.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |  and lower(mb.blocked_username) = lower(?)
      |""".stripMargin

  val removeBlockSql: String =
    """
      |delete from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin
