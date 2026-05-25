package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.codec.BlogHttpCodecs.given
import cats.effect.IO
import domains.blog.application.BlogCommands
import domains.blog.model.request.CreateBlogRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateBlog:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" =>
        context.handlers.executeDecoded[CreateBlogRequest, BlogCommands.CreateBlogResult](
          request,
          context.plans.createBlog
        )
    }
