package domains.problem.http.api

import domains.problem.http.mapper.ProblemHttpResponseMappers
import domains.problem.http.mapper.ProblemHttpRequestMappers



import domains.problem.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblemSuggestions:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" / "suggestions" =>
        ProblemHttpRequestMappers.problemSearchQuery(request.uri.query.params) match
          case Left(message) =>
            ProblemHttpResponseMappers.validationErrorResponse(message)
          case Right(query) =>
            context.handlers.execute(request, query, context.plans.listProblemSuggestions)
    }
