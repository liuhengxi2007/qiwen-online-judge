package domains.notification.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.notification.http.api.ListNotifications
import domains.notification.http.api.GetNotificationUnreadCount
import domains.notification.http.api.MarkNotificationRead
import domains.notification.http.api.MarkAllNotificationsRead
import domains.notification.http.api.SubscribeNotificationEvents
import domains.auth.application.SessionStore
import domains.notification.application.NotificationEventHub
import org.http4s.HttpRoutes

object NotificationRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    ListNotifications.routes(databaseSession, sessionStore, notificationEventHub) <+>
      GetNotificationUnreadCount.routes(databaseSession, sessionStore, notificationEventHub) <+>
      MarkNotificationRead.routes(databaseSession, sessionStore, notificationEventHub) <+>
      MarkAllNotificationsRead.routes(databaseSession, sessionStore, notificationEventHub) <+>
      SubscribeNotificationEvents.routes(databaseSession, sessionStore, notificationEventHub)
