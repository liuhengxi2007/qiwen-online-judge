package domains.message.http.api

import domains.message.http.response.MessageHttpResponses



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.user.model.Username
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, RemoveBlockResult}
import domains.message.application.input.{CreateConversationRequest, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.message.model.{MessageConversationId, MessageId}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetConversationHistory:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            val beforeMessageId = request.uri.query.params.get("before").flatMap(rawId => MessageId.parse(rawId).toOption)
            val limit = request.uri.query.params.get("limit").flatMap(_.toIntOption)
            context.handlers.execute(
              request,
              MessageHttpPlans.HistoryInput(parsedConversationId, beforeMessageId, limit),
              context.plans.getConversationHistory
            )
    }
