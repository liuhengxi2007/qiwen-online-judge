package domains.notification.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.notification.utils.NotificationEventHub
import domains.notification.api.*
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
