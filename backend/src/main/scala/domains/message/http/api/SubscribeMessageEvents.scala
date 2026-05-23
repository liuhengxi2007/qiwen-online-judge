package domains.message.http.api



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.message.application.MessageStreamEvent
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object SubscribeMessageEvents:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "messages" / "events" =>
        AuthHttpSessionSupport.withAuthenticatedUser(context.databaseSession, context.sessionStore, request) { actor =>
          IO.pure(
            Response[IO](status = Status.Ok)
              .putHeaders(
                Header.Raw(CIString("Content-Type"), "text/event-stream"),
                Header.Raw(CIString("Cache-Control"), "no-cache")
              )
              .withBodyStream(
                context.messageEventHub.subscribe(actor.username).map(toServerSentEventString).through(text.utf8.encode)
              )
          )
        }
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
