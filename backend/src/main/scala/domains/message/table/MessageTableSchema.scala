package domains.message.table

import cats.effect.IO

import java.sql.Connection

object MessageTableSchema:

  val initConversationTableSql: String =
    """
      |create table if not exists message_conversations (
      |  id uuid primary key,
      |  participant_a_username varchar(120) not null references auth_users(username) on delete cascade,
      |  participant_b_username varchar(120) not null references auth_users(username) on delete cascade,
      |  created_at timestamp not null,
      |  updated_at timestamp not null,
      |  last_message_at timestamp not null,
      |  constraint message_conversations_distinct_participants_check check (lower(participant_a_username) <> lower(participant_b_username))
      |);
      |""".stripMargin

  val createConversationPairIndexSql: String =
    """
      |create unique index if not exists message_conversations_participants_pair_idx
      |on message_conversations (lower(participant_a_username), lower(participant_b_username))
      |""".stripMargin

  val createConversationLastMessageIndexSql: String =
    """
      |create index if not exists message_conversations_last_message_at_idx
      |on message_conversations (last_message_at desc, created_at desc)
      |""".stripMargin

  val initDirectMessageTableSql: String =
    """
      |create table if not exists direct_messages (
      |  id uuid primary key,
      |  conversation_id uuid not null references message_conversations(id) on delete cascade,
      |  sender_username varchar(120) not null references auth_users(username) on delete cascade,
      |  recipient_username varchar(120) not null references auth_users(username) on delete cascade,
      |  content text not null,
      |  created_at timestamp not null,
      |  read_at timestamp null
      |);
      |""".stripMargin

  val createDirectMessagesConversationIndexSql: String =
    """
      |create index if not exists direct_messages_conversation_created_at_idx
      |on direct_messages (conversation_id, created_at desc, id desc)
      |""".stripMargin

  val createDirectMessagesRecipientIndexSql: String =
    """
      |create index if not exists direct_messages_recipient_read_at_idx
      |on direct_messages (recipient_username, read_at, created_at desc)
      |""".stripMargin

  val initBlockTableSql: String =
    """
      |create table if not exists message_blocks (
      |  owner_username varchar(120) not null references auth_users(username) on delete cascade,
      |  blocked_username varchar(120) not null references auth_users(username) on delete cascade,
      |  created_at timestamp not null,
      |  primary key (owner_username, blocked_username),
      |  constraint message_blocks_distinct_users_check check (lower(owner_username) <> lower(blocked_username))
      |);
      |""".stripMargin

  val createBlockOwnerIndexSql: String =
    """
      |create index if not exists message_blocks_owner_idx
      |on message_blocks (owner_username, created_at desc)
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initConversationTableSql)
        statement.execute(createConversationPairIndexSql)
        statement.execute(createConversationLastMessageIndexSql)
        statement.execute(initDirectMessageTableSql)
        statement.execute(createDirectMessagesConversationIndexSql)
        statement.execute(createDirectMessagesRecipientIndexSql)
        statement.execute(initBlockTableSql)
        statement.execute(createBlockOwnerIndexSql)
      finally statement.close()
    }
