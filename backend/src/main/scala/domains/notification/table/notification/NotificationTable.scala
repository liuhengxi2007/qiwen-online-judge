package domains.notification.table.notification



import cats.effect.IO
import domains.user.model.Username
import domains.notification.model.{NotificationId, NotificationKind, NotificationPayload}
import domains.notification.application.output.{NotificationListResponse}
import domains.notification.table.notification.NotificationTableSupport.{encodeNotificationKindColumn, readNotificationSummary}
import shared.model.PageRequest

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID
import database.utils.UserIdentitySql

object NotificationTable:

  def initialize(connection: Connection): IO[Unit] =
    NotificationTableSchema.initialize(connection)

  private val insertSQL: String =
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

  def insert(
    connection: Connection,
    recipientUsername: Username,
    actorUsername: Option[Username],
    kind: NotificationKind,
    titleKey: String,
    bodyKey: String,
    payload: NotificationPayload,
    targetPath: String,
    targetAnchor: Option[String]
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, NotificationId(UUID.randomUUID()).value)
        statement.setString(2, recipientUsername.value)
        actorUsername match
          case Some(username) => statement.setString(3, username.value)
          case None => statement.setNull(3, java.sql.Types.VARCHAR)
        statement.setString(4, encodeNotificationKindColumn(kind))
        statement.setString(5, titleKey)
        statement.setString(6, bodyKey)
        statement.setString(7, NotificationPayloadJsonCodec.encode(payload))
        statement.setString(8, targetPath)
        targetAnchor match
          case Some(anchor) => statement.setString(9, anchor)
          case None => statement.setNull(9, java.sql.Types.VARCHAR)
        statement.setTimestamp(10, Timestamp.from(Instant.now()))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val listByRecipientSQL: String =
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
      |limit ? offset ?
      |""".stripMargin

  def listForRecipient(connection: Connection, recipientUsername: Username, pageRequest: PageRequest): IO[NotificationListResponse] =
    val normalizedPageRequest = pageRequest.normalized
    for
      notifications <- IO.blocking {
        val statement = connection.prepareStatement(listByRecipientSQL)
        try
          statement.setString(1, recipientUsername.value)
          statement.setInt(2, normalizedPageRequest.pageSize)
          statement.setInt(3, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readNotificationSummary(resultSet)).toList
          finally resultSet.close()
        finally statement.close()
      }
      unreadCount <- countUnreadForRecipient(connection, recipientUsername)
      totalItems <- countForRecipient(connection, recipientUsername)
    yield NotificationListResponse(notifications, unreadCount, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val countByRecipientSQL: String =
    """
      |select count(*) as total_items
      |from notifications
      |where recipient_username = ?
      |""".stripMargin

  def countForRecipient(connection: Connection, recipientUsername: Username): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(countByRecipientSQL)
      try
        statement.setString(1, recipientUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getLong("total_items") else 0L
        finally resultSet.close()
      finally statement.close()
    }

  private val countUnreadSQL: String =
    """
      |select count(*) as unread_count
      |from notifications
      |where recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin

  def countUnreadForRecipient(connection: Connection, recipientUsername: Username): IO[Int] =
    IO.blocking {
      val statement = connection.prepareStatement(countUnreadSQL)
      try
        statement.setString(1, recipientUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getInt("unread_count") else 0
        finally resultSet.close()
      finally statement.close()
    }

  private val markReadSQL: String =
    """
      |update notifications
      |set status = 'read',
      |    read_at = ?
      |where id = ?
      |  and recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin

  def markRead(connection: Connection, notificationId: NotificationId, recipientUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(markReadSQL)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.setObject(2, notificationId.value)
        statement.setString(3, recipientUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val markAllReadSQL: String =
    """
      |update notifications
      |set status = 'read',
      |    read_at = ?
      |where recipient_username = ?
      |  and status = 'unread'
      |""".stripMargin

  def markAllRead(connection: Connection, recipientUsername: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(markAllReadSQL)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.setString(2, recipientUsername.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
