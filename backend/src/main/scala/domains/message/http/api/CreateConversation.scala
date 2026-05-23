package domains.message.http.api



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

object CreateConversation:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" =>
        context.handlers.executeDecoded[CreateConversationRequest, CreateConversationRequest, CreateConversationResult](
          request,
          context.plans.createConversation
        )(identity)
    }
