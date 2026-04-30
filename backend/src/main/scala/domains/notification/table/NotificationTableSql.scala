package domains.notification.table

import domains.shared.sql.UserIdentitySql

object NotificationTableSql:

  val insertSql: String =
    """
      |insert into notifications (
      |  id,
      |  recipient_username,
      |  actor_username,
      |  kind,
      |  status,
      |  title_key,
      |  body_key,
      |  payload_json,
      |  target_path,
      |  target_anchor,
      |  created_at
      |) values (?, ?, ?, ?, 'unread', ?, ?, cast(? as jsonb), ?, ?, ?)
      |""".stripMargin

  val listByRecipientSql: String =
    s"""
      |select n.id,
      |       n.kind,
      |       n.title_key,
      |       n.body_key,
      |       n.payload_json::text as payload_json,
      |       n.target_path,
      |       n.target_anchor,
      |       n.status,
      |       n.created_at,
      |       ${UserIdentitySql.selectColumns("n.actor_username", "actor", "actor_user")}
      |from notifications n
      |left join auth_users actor_user on actor_user.username = n.actor_username
      |where n.recipient_username = ?
      |order by n.created_at desc, n.id desc
      |""".stripMargin

  val countUnreadSql: String =
    """
      |select count(*) as unread_count
      |from notifications
      |where recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin

  val markReadSql: String =
    """
      |update notifications
      |set status = 'read',
      |    read_at = ?
      |where id = ?
      |  and recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin

  val markAllReadSql: String =
    """
      |update notifications
      |set status = 'read',
      |    read_at = ?
      |where recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin
