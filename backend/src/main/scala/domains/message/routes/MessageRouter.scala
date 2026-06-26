package domains.message.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.message.api.MessageEventHubContext
import domains.message.api.*
import org.http4s.HttpRoutes

/** 汇总私信 domain 的 http4s 路由，负责把消息事件中心注入发送、已读和订阅 API。 */
object MessageRouter:

  /** 构造私信相关 HTTP 路由，SSE 和读回执共享同一个 MessageEventHub。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStoreContext, messageEventHub: MessageEventHubContext): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      context,
      List(
        ListInbox,
        GetConversationHistory,
        CreateConversation,
        SendDirectMessage(messageEventHub),
        MarkConversationRead(messageEventHub),
        MarkAllMessagesRead(messageEventHub),
        ListMessageBlocks,
        AddMessageBlock(messageEventHub),
        RemoveMessageBlock(messageEventHub),
        SubscribeMessageEvents(messageEventHub)
      )
    )
