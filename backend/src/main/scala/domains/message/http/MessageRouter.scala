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
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object MessageRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, messageEventHub: MessageEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = MessageHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      messageEventHub = messageEventHub,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore),
      plans = MessageHttpPlanDefinitions.plans(messageEventHub)
    )

    val endpointRoutes = List(
      ListInbox.routes(context),
      GetConversationHistory.routes(context),
      CreateConversation.routes(context),
      SendDirectMessage.routes(context),
      MarkConversationRead.routes(context),
      MarkAllMessagesRead.routes(context),
      ListMessageBlocks.routes(context),
      AddMessageBlock.routes(context),
      RemoveMessageBlock.routes(context),
      SubscribeMessageEvents.routes(context)
    )

    endpointRoutes.reduce(_ <+> _)
