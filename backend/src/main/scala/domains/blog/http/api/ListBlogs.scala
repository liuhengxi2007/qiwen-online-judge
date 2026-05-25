package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.mapper.BlogHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListBlogs:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "blogs" =>
        context.handlers.execute(
          request,
          BlogHttpRequestMappers.listBlogsInput(request.uri.query.params),
          context.plans.listBlogs
        )
    }
