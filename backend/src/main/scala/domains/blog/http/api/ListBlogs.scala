package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import domains.user.model.Username
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListBlogs:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "blogs" =>
        context.handlers.execute(
          request,
          BlogHttpPlans.ListBlogsInput(
            request.uri.query.params.get("username").map(Username.canonical),
            PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
          ),
          context.plans.listBlogs
        )
    }