package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.mapper.BlogHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListPendingProblemBlogs:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" =>
        BlogHttpRequestMappers.problemBlogsInput(rawProblemSlug, request.uri.query.params) match
          case Left(message) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(input) =>
            context.handlers.execute(request, input, context.plans.listPendingProblemBlogs)
    }
