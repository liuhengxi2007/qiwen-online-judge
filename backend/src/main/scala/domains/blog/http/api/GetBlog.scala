package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import domains.blog.model.BlogId
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetBlog:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "blogs" / rawBlogId =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            context.handlers.execute(request, blogId, context.plans.getBlog)
    }
