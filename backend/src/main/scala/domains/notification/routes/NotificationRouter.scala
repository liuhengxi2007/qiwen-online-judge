package domains.notification.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.notification.utils.NotificationEventHub
import domains.notification.api.*
import org.http4s.HttpRoutes

/** 汇总通知 domain 的 http4s 路由，负责把通知事件中心注入已读和订阅 API。 */
object NotificationRouter:

  /** 构造通知相关 HTTP 路由，内部创建通知 API 与用户可见通知 API 共用同一上下文。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListNotifications,
        GetNotificationUnreadCount,
        MarkNotificationRead(notificationEventHub),
        MarkAllNotificationsRead(notificationEventHub),
        SubscribeNotificationEvents(notificationEventHub),
        CreateNotification
      )
    )
