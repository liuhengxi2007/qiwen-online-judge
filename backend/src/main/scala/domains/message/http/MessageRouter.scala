package domains.message.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.message.http.api.ListInbox
import domains.message.http.api.GetConversationHistory
import domains.message.http.api.CreateConversation
import domains.message.http.api.SendDirectMessage
import domains.message.http.api.MarkConversationRead
import domains.message.http.api.MarkAllMessagesRead
import domains.message.http.api.ListMessageBlocks
import domains.message.http.api.AddMessageBlock
import domains.message.http.api.RemoveMessageBlock
import domains.message.http.api.SubscribeMessageEvents
import domains.auth.application.SessionStore
import domains.message.application.MessageEventHub
import org.http4s.HttpRoutes

object MessageRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, messageEventHub: MessageEventHub): HttpRoutes[IO] =
    val endpointRoutes = List(
      ListInbox.routes(databaseSession, sessionStore, messageEventHub),
      GetConversationHistory.routes(databaseSession, sessionStore, messageEventHub),
      CreateConversation.routes(databaseSession, sessionStore, messageEventHub),
      SendDirectMessage.routes(databaseSession, sessionStore, messageEventHub),
      MarkConversationRead.routes(databaseSession, sessionStore, messageEventHub),
      MarkAllMessagesRead.routes(databaseSession, sessionStore, messageEventHub),
      ListMessageBlocks.routes(databaseSession, sessionStore, messageEventHub),
      AddMessageBlock.routes(databaseSession, sessionStore, messageEventHub),
      RemoveMessageBlock.routes(databaseSession, sessionStore, messageEventHub),
      SubscribeMessageEvents.routes(databaseSession, sessionStore, messageEventHub)
    )

    endpointRoutes.reduce(_ <+> _)
