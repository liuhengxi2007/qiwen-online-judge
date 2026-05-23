package domains.message.http.api



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.user.model.Username
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, RemoveBlockResult}
import domains.message.application.{MessageEventHub, MessageStreamEvent}
import domains.message.application.input.{CreateConversationRequest, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.message.model.{MessageConversationId, MessageId}
import shared.http.AuthenticatedHttpExecutor
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object CreateConversation:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, messageEventHub: MessageEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = MessageHttpPlanDefinitions.plans(messageEventHub)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" =>
        handlers.executeDecoded[CreateConversationRequest, CreateConversationRequest, CreateConversationResult](
          request,
          plans.createConversation
        )(identity)
    }
  private given Encoder[MessageStreamEvent] = Encoder.instance {
    case MessageStreamEvent.MessageReceived(message) =>
      message.asJson
    case MessageStreamEvent.ConversationRead(conversationId, readUpToMessageId, readerUsername) =>
      io.circe.Json.obj(
        "conversationId" -> conversationId.asJson,
        "readUpToMessageId" -> readUpToMessageId.asJson,
        "readerUsername" -> readerUsername.asJson
      )
    case MessageStreamEvent.InboxChanged =>
      io.circe.Json.obj()
  }

  private def toServerSentEvent(event: MessageStreamEvent): ServerSentEvent =
    val eventName = event match
      case _: MessageStreamEvent.MessageReceived => "message_received"
      case _: MessageStreamEvent.ConversationRead => "conversation_read"
      case MessageStreamEvent.InboxChanged => "inbox_changed"

    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some(eventName))

  private def toServerSentEventString(event: MessageStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
