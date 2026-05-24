package domains.message.http.api



import domains.message.http.*
import cats.effect.IO
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListInbox:

  def routes(context: MessageHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "messages" / "inbox" =>
        context.handlers.execute(request, PageRequestQuerySupport.parsePageRequest(request.uri.query.params), context.plans.listInbox)
    }
