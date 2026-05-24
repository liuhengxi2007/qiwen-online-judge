package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses



import domains.problem.http.*
import cats.effect.IO
import domains.problem.model.{ProblemSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ClearProblemData:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "clear" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            context.handlers.execute(request, parsedProblemSlug, context.plans.clearProblemData)
    }
