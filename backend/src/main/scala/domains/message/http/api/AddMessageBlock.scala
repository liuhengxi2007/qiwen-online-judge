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

object AddMessageBlock:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "blocks" / targetUsername =>
        context.handlers.execute(
          request,
          Username.canonical(targetUsername),
          context.plans.addBlock
        )
    }
