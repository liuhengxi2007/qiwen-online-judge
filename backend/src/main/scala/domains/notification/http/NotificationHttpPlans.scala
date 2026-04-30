package domains.notification.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.notification.application.{NotificationCommands, NotificationEventHub, NotificationStreamEvent}
import domains.notification.model.NotificationId
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}

import java.sql.Connection

object NotificationHttpPlans:

  case object ListNotifications extends PlainAuthenticatedHttpPlan[Unit, domains.notification.model.NotificationListResponse]:
    override val name: String = "ListNotifications"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[domains.notification.model.NotificationListResponse] =
      val _ = input
      NotificationCommands.listNotifications(databaseSession, actor)

  case object GetUnreadCount extends PlainAuthenticatedHttpPlan[Unit, domains.notification.model.NotificationUnreadCountResponse]:
    override val name: String = "GetUnreadCount"
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[domains.notification.model.NotificationUnreadCountResponse] =
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
