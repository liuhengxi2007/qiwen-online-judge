package domains.usergroup.http.api



import domains.usergroup.http.*
import cats.effect.IO
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUserGroups:

  def routes(context: UserGroupHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" =>
        context.handlers.execute(request, PageRequestQuerySupport.parsePageRequest(request.uri.query.params), UserGroupHttpPlanDefinitions.listUserGroups)
    }