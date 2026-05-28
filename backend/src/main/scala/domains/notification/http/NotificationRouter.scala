package domains.notification.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.notification.application.NotificationEventHub
import domains.notification.http.api.*
import org.http4s.HttpRoutes

object NotificationRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListNotifications,
        GetNotificationUnreadCount,
        MarkNotificationRead(notificationEventHub),
        MarkAllNotificationsRead(notificationEventHub),
        SubscribeNotificationEvents(notificationEventHub)
      )
    )
