package routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.auth.api.SessionStoreContext
import domains.message.api.MessageEventHubContext
import domains.notification.api.NotificationEventHubContext
import org.http4s.HttpRoutes

/** 合并实时事件传输的路由集合。 */
object RealtimeRoutes:

  /** 注入现有两个事件中心并构造合并 SSE 路由。 */
  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStoreContext,
    messageEventHub: MessageEventHubContext,
    notificationEventHub: NotificationEventHubContext
  ): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      context,
      List(
        SubscribeRealtimeEvents(messageEventHub, notificationEventHub)
      )
    )
