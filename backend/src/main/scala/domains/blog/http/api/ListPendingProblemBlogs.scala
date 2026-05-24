package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import domains.problem.model.ProblemSlug
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListPendingProblemBlogs:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            context.handlers.execute(request, BlogHttpPlans.ProblemBlogsInput(problemSlug, PageRequestQuerySupport.parsePageRequest(request.uri.query.params)), context.plans.listPendingProblemBlogs)
    }
