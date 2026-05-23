package domains.notification.table.notification



import cats.effect.IO

import java.sql.Connection

object NotificationTableSchema:

  val initTableSql: String =
    """
      |create table if not exists notifications (
      |  id uuid primary key,
      |  recipient_username varchar(120) not null references auth_users(username) on delete cascade,
      |  actor_username varchar(120) references auth_users(username) on delete set null,
      |  kind varchar(64) not null,
      |  status varchar(16) not null default 'unread',
      |  title_key varchar(160) not null,
      |  body_key varchar(160) not null,
      |  payload_json jsonb not null,
      |  target_path varchar(512) not null,
      |  target_anchor varchar(255),
      |  read_at timestamp,
      |  created_at timestamp not null
      |);
      |""".stripMargin

  val addStatusCheckSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'notifications_status_check'
      |  ) then
      |    alter table notifications add constraint notifications_status_check check (status in ('unread', 'read'));
      |  end if;
      |end $$;
      |""".stripMargin

  val createRecipientCreatedAtIndexSql: String =
    """
      |create index if not exists notifications_recipient_created_at_idx
      |on notifications(recipient_username, created_at desc)
      |""".stripMargin

  val createUnreadIndexSql: String =
    """
      |create index if not exists notifications_recipient_status_idx
      |on notifications(recipient_username, status)
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(addStatusCheckSql)
        statement.execute(createRecipientCreatedAtIndexSql)
        statement.execute(createUnreadIndexSql)
      finally statement.close()
    }
