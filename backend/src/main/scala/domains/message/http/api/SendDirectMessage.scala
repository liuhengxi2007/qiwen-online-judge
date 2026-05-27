package domains.message.http.api

import domains.message.http.mapper.MessageHttpResponseMappers
import domains.message.http.mapper.MessageHttpRequestMappers



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.message.objects.request.SendDirectMessageRequest
import domains.message.objects.MessageConversationId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object SendDirectMessage:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageHttpRequestMappers.conversationId(conversationId) match
          case Left(message) => MessageHttpResponseMappers.validationErrorResponse(message)
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
