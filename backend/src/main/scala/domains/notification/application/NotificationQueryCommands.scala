package domains.notification.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.notification.objects.response.{NotificationListResponse, NotificationUnreadCountResponse}
import domains.notification.table.notification.NotificationTable
import shared.objects.PageRequest

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
