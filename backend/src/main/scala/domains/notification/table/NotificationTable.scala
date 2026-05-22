package domains.notification.table



import cats.effect.IO
import domains.user.model.Username
import domains.notification.model.{NotificationId, NotificationKind, NotificationPayload}
import domains.notification.application.output.{NotificationListResponse, NotificationSummary}
import domains.notification.table.NotificationTableSql.*
import domains.notification.table.utils.NotificationTableSupport.{encodeNotificationKindColumn, readNotificationSummary}
import domains.notification.table.utils.NotificationPayloadJsonCodec
import shared.model.PageRequest

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object NotificationTable:

  def initialize(connection: Connection): IO[Unit] =
    NotificationTableSchema.initialize(connection)

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
      val statement = connection.prepareStatement(insertSql)
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

  def listForRecipient(connection: Connection, recipientUsername: Username, pageRequest: PageRequest): IO[NotificationListResponse] =
    val normalizedPageRequest = pageRequest.normalized
    for
      notifications <- IO.blocking {
        val statement = connection.prepareStatement(listByRecipientSql)
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

  def countForRecipient(connection: Connection, recipientUsername: Username): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(countByRecipientSql)
      try
        statement.setString(1, recipientUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getLong("total_items") else 0L
        finally resultSet.close()
      finally statement.close()
    }

  def countUnreadForRecipient(connection: Connection, recipientUsername: Username): IO[Int] =
    IO.blocking {
      val statement = connection.prepareStatement(countUnreadSql)
      try
        statement.setString(1, recipientUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getInt("unread_count") else 0
        finally resultSet.close()
      finally statement.close()
    }

  def markRead(connection: Connection, notificationId: NotificationId, recipientUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(markReadSql)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.setObject(2, notificationId.value)
        statement.setString(3, recipientUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def markAllRead(connection: Connection, recipientUsername: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(markAllReadSql)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.setString(2, recipientUsername.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
