package domains.message.http.api



import domains.message.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object MarkAllMessagesRead:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "messages" / "read-all" =>
        context.handlers.execute(
          request,
          (),
          context.plans.markAllMessagesRead
        )
    }
