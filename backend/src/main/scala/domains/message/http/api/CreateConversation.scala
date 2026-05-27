package domains.message.http.api



import domains.message.http.*
import domains.message.http.codec.MessageHttpCodecs.given
import cats.effect.IO
import domains.message.application.MessageCommandResults.CreateConversationResult
import domains.message.objects.request.CreateConversationRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateConversation:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "conversations" =>
        context.handlers.executeDecoded[CreateConversationRequest, CreateConversationResult](
          request,
          context.plans.createConversation
        )
    }
