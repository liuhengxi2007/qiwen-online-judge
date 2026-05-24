package domains.message.http.api

import domains.message.http.response.MessageHttpResponses



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.message.application.input.MarkConversationReadRequest
import domains.message.model.MessageConversationId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object MarkConversationRead:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" / conversationId / "read" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            context.handlers.executeDecoded[
              MarkConversationReadRequest,
              (MessageConversationId, MarkConversationReadRequest),
              MessageHttpPlans.MarkConversationReadOutput
            ](
              request,
              context.plans.markConversationRead
            )(body => (parsedConversationId, body))
    }
