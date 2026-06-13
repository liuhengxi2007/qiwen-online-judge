package domains.notification.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.notification.utils.NotificationEventHubContext
import domains.notification.api.*
import org.http4s.HttpRoutes

/** 汇总通知 domain 的 http4s 路由，负责把通知事件中心注入已读和订阅 API。 */
object NotificationRouter:

  /** 构造通知相关 HTTP 路由，内部创建通知 API 与用户可见通知 API 共用同一上下文。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStoreContext, notificationEventHub: NotificationEventHubContext): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

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
