package domains.message.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.message.utils.MessageEventHub
import domains.message.api.*
import org.http4s.HttpRoutes

object MessageRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, messageEventHub: MessageEventHub): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

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
