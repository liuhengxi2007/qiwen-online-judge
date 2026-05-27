package domains.notification.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.notification.application.{NotificationCommands, NotificationEventHub, NotificationStreamEvent}
import domains.notification.objects.NotificationId
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import shared.objects.PageRequest

import java.sql.Connection

object NotificationHttpPlans:

  case object ListNotifications extends PlainAuthenticatedHttpPlan[AuthUser, PageRequest, domains.notification.objects.response.NotificationListResponse]:
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: PageRequest): IO[domains.notification.objects.response.NotificationListResponse] =
      NotificationCommands.listNotifications(databaseSession, actor, input)

  case object GetUnreadCount extends PlainAuthenticatedHttpPlan[AuthUser, Unit, domains.notification.objects.response.NotificationUnreadCountResponse]:
    override def execute(databaseSession: DatabaseSession, actor: AuthUser, input: Unit): IO[domains.notification.objects.response.NotificationUnreadCountResponse] =
      val _ = input
      NotificationCommands.getUnreadCount(databaseSession, actor)

  final class MarkNotificationReadPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, NotificationId, NotificationCommands.MarkNotificationReadResult]:
    override def execute(connection: Connection, actor: AuthUser, input: NotificationId): IO[NotificationCommands.MarkNotificationReadResult] =
      NotificationCommands.markNotificationRead(connection, actor, input).flatTap {
        case NotificationCommands.MarkNotificationReadResult.Marked =>
          notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
        case NotificationCommands.MarkNotificationReadResult.NotFound =>
          IO.unit
      }

  final class MarkAllNotificationsReadPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, Unit, NotificationCommands.MarkAllNotificationsReadResult]:
    override def execute(connection: Connection, actor: AuthUser, input: Unit): IO[NotificationCommands.MarkAllNotificationsReadResult] =
      val _ = input
      NotificationCommands.markAllNotificationsRead(connection, actor).flatTap(_ =>
        notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
      )
