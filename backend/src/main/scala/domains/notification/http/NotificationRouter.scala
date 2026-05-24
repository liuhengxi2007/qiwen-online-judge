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
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object NotificationRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = NotificationHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      notificationEventHub = notificationEventHub,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore),
      plans = NotificationHttpPlanDefinitions.plans(notificationEventHub)
    )

    ListNotifications.routes(context) <+>
      GetNotificationUnreadCount.routes(context) <+>
      MarkNotificationRead.routes(context) <+>
      MarkAllNotificationsRead.routes(context) <+>
      SubscribeNotificationEvents.routes(context)
