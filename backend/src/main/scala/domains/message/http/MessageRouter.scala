package domains.message.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.message.application.MessageEventHub
import domains.message.http.api.*
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
