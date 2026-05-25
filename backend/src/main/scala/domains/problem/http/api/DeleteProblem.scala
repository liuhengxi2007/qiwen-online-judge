package domains.problem.http.api

import domains.problem.http.mapper.ProblemHttpResponseMappers
import domains.problem.http.mapper.ProblemHttpRequestMappers



import domains.problem.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object DeleteProblem:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemHttpRequestMappers.problemSlug(problemSlug) match
          case Left(message) =>
            ProblemHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            context.handlers.execute(request, parsedProblemSlug, context.plans.deleteProblem)
    }
