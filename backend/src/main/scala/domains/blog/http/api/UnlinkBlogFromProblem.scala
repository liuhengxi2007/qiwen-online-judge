package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.mapper.BlogHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UnlinkBlogFromProblem:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-links" / rawBlogId / "delete" =>
        BlogHttpRequestMappers.blogProblemLinkInput(rawProblemSlug, rawBlogId) match
          case Left(message) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(input) =>
            context.handlers.execute(request, input, context.plans.unlinkBlogFromProblem)
    }
