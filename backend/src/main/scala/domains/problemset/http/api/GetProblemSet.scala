package domains.problemset.http.api

import domains.problemset.http.response.ProblemSetHttpResponses



import domains.problemset.http.*
import cats.effect.IO
import domains.problemset.model.{ProblemSetSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetProblemSet:

  def routes(context: ProblemSetHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            context.handlers.execute(request, parsedProblemSetSlug, ProblemSetHttpPlanDefinitions.getProblemSet)
    }