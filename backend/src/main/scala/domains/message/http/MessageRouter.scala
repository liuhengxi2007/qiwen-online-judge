package domains.message.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.Username
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, RemoveBlockResult}
import domains.message.application.{MessageEventHub, MessageStreamEvent}
import domains.message.model.{CreateConversationRequest, MarkConversationReadRequest, MessageConversationId, MessageId, SendDirectMessageRequest}
import domains.shared.http.AuthenticatedHttpExecutor
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object MessageRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, messageEventHub: MessageEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = MessageHttpPlanDefinitions.plans(messageEventHub)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "messages" / "inbox" =>
        handlers.execute(request, (), plans.listInbox)

      case request @ GET -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            val beforeMessageId = request.uri.query.params.get("before").flatMap(rawId => MessageId.parse(rawId).toOption)
            val limit = request.uri.query.params.get("limit").flatMap(_.toIntOption)
            handlers.execute(
              request,
              MessageHttpPlans.HistoryInput(parsedConversationId, beforeMessageId, limit),
              plans.getConversationHistory
            )

      case request @ POST -> Root / "api" / "messages" / "conversations" =>
        handlers.executeDecoded[CreateConversationRequest, CreateConversationRequest, CreateConversationResult](
          request,
          plans.createConversation
        )(identity)

      case request @ POST -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            handlers.executeDecoded[
              SendDirectMessageRequest,
              (MessageConversationId, SendDirectMessageRequest),
              MessageHttpPlans.SendMessageOutput
            ](
              request,
              plans.sendMessage
            )(body => (parsedConversationId, body))

      case request @ POST -> Root / "api" / "messages" / "conversations" / conversationId / "read" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            handlers.executeDecoded[
              MarkConversationReadRequest,
              (MessageConversationId, MarkConversationReadRequest),
              MessageHttpPlans.MarkConversationReadOutput
            ](
              request,
              plans.markConversationRead
            )(body => (parsedConversationId, body))

      case request @ POST -> Root / "api" / "messages" / "read-all" =>
        handlers.execute(
          request,
          (),
          plans.markAllMessagesRead
        )

      case request @ GET -> Root / "api" / "messages" / "blocks" =>
        handlers.execute(
          request,
          (),
          plans.listBlocks
        )

      case request @ POST -> Root / "api" / "messages" / "blocks" / targetUsername =>
        handlers.execute(
          request,
          Username.canonical(targetUsername),
          plans.addBlock
        )

      case request @ POST -> Root / "api" / "messages" / "blocks" / targetUsername / "remove" =>
        handlers.execute(
          request,
          Username.canonical(targetUsername),
          plans.removeBlock
        )

      case request @ GET -> Root / "api" / "messages" / "events" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          IO.pure(
            Response[IO](status = Status.Ok)
              .putHeaders(
                Header.Raw(CIString("Content-Type"), "text/event-stream"),
                Header.Raw(CIString("Cache-Control"), "no-cache")
              )
              .withBodyStream(
                messageEventHub.subscribe(actor.username).map(toServerSentEventString).through(text.utf8.encode)
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
