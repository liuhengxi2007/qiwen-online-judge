package domains.notification.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.notification.model.{NotificationListResponse, NotificationUnreadCountResponse}
import domains.notification.table.NotificationTable

object NotificationQueryCommands:

  def listNotifications(
    databaseSession: DatabaseSession,
    actor: AuthUser
  ): IO[NotificationListResponse] =
    databaseSession.withTransactionConnection(connection => NotificationTable.listForRecipient(connection, actor.username))

  def getUnreadCount(
    databaseSession: DatabaseSession,
    actor: AuthUser
  ): IO[NotificationUnreadCountResponse] =
    databaseSession.withTransactionConnection(connection => NotificationTable.countUnreadForRecipient(connection, actor.username).map(NotificationUnreadCountResponse(_)))
