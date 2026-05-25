package domains.message.http.api

import domains.message.http.mapper.MessageHttpResponseMappers
import domains.message.http.mapper.MessageHttpRequestMappers



import domains.message.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetConversationHistory:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "messages" / "conversations" / conversationId / "messages" =>
        MessageHttpRequestMappers.historyInput(conversationId, request.uri.query.params) match
          case Left(message) => MessageHttpResponseMappers.validationErrorResponse(message)
          case Right(input) =>
            context.handlers.execute(
              request,
              input,
              context.plans.getConversationHistory
            )
    }
