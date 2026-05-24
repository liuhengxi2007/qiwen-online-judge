package domains.message.http.api

import domains.message.http.response.MessageHttpResponses



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.message.application.input.SendDirectMessageRequest
import domains.message.model.MessageConversationId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object SendDirectMessage:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageConversationId.parse(conversationId) match
          case Left(message) => MessageHttpResponses.validationErrorResponse(message)
          case Right(parsedConversationId) =>
            context.handlers.executeDecoded[
              SendDirectMessageRequest,
              (MessageConversationId, SendDirectMessageRequest),
              MessageHttpPlans.SendMessageOutput
            ](
              request,
              context.plans.sendMessage
            )(body => (parsedConversationId, body))
    }
