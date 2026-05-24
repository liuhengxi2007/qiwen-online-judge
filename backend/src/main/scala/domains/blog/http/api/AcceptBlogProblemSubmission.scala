package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import domains.blog.model.BlogId
import domains.problem.model.ProblemSlug
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AcceptBlogProblemSubmission:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" / rawBlogId / "accept" =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            context.handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), context.plans.acceptBlogProblemSubmission)
    }
