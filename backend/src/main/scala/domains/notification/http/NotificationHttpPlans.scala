package domains.notification.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.notification.application.{NotificationCommands, NotificationEventHub, NotificationStreamEvent}
import domains.notification.model.NotificationId
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.shared.model.PageRequest

import java.sql.Connection

object NotificationHttpPlans:

  case object ListNotifications extends PlainAuthenticatedHttpPlan[PageRequest, domains.notification.application.view.NotificationListResponse]:
    override val name: String = "ListNotifications"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: PageRequest): IO[domains.notification.application.view.NotificationListResponse] =
      NotificationCommands.listNotifications(databaseSession, actor, input)

  case object GetUnreadCount extends PlainAuthenticatedHttpPlan[Unit, domains.notification.application.view.NotificationUnreadCountResponse]:
    override val name: String = "GetUnreadCount"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[domains.notification.application.view.NotificationUnreadCountResponse] =
      val _ = input
      NotificationCommands.getUnreadCount(databaseSession, actor)

  final class MarkNotificationReadPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[NotificationId, NotificationCommands.MarkNotificationReadResult]:
    override val name: String = "MarkNotificationRead"
    override def execute(connection: Connection, actor: AuthUser, input: NotificationId): IO[NotificationCommands.MarkNotificationReadResult] =
      NotificationCommands.markNotificationRead(connection, actor, input).flatTap {
        case NotificationCommands.MarkNotificationReadResult.Marked =>
          notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
        case NotificationCommands.MarkNotificationReadResult.NotFound =>
          IO.unit
      }

  final class MarkAllNotificationsReadPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[Unit, NotificationCommands.MarkAllNotificationsReadResult]:
    override val name: String = "MarkAllNotificationsRead"
    override def execute(connection: Connection, actor: AuthUser, input: Unit): IO[NotificationCommands.MarkAllNotificationsReadResult] =
      val _ = input
      NotificationCommands.markAllNotificationsRead(connection, actor).flatTap(_ =>
        notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
      )
