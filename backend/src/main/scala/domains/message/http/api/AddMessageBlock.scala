package domains.message.http.api



import domains.message.http.*
import domains.message.http.mapper.MessageHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AddMessageBlock:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "blocks" / targetUsername =>
        context.handlers.execute(
          request,
          MessageHttpRequestMappers.username(targetUsername),
          context.plans.addBlock
        )
    }
