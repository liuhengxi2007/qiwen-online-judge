package domains.notification.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.notification.application.output.{NotificationListResponse, NotificationUnreadCountResponse}
import domains.notification.table.NotificationTable
import domains.shared.model.PageRequest

object NotificationQueryCommands:

  def listNotifications(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[NotificationListResponse] =
    databaseSession.withTransactionConnection(connection => NotificationTable.listForRecipient(connection, actor.username, pageRequest.normalized))

  def getUnreadCount(
    databaseSession: DatabaseSession,
    actor: AuthUser
  ): IO[NotificationUnreadCountResponse] =
    databaseSession.withTransactionConnection(connection => NotificationTable.countUnreadForRecipient(connection, actor.username).map(NotificationUnreadCountResponse(_)))
