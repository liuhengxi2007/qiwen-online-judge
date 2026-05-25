package domains.problemset.http.api

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers
import domains.problemset.http.mapper.ProblemSetHttpRequestMappers



import domains.problemset.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object RemoveProblemFromProblemSet:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" / linkedProblemSlug / "remove" =>
        ProblemSetHttpRequestMappers.removeProblemInput(problemSetSlug, linkedProblemSlug) match
          case Left(message) =>
            ProblemSetHttpResponseMappers.validationErrorResponse(message)
          case Right(input) =>
            handlers.execute(request, input, ProblemSetHttpPlanDefinitions.removeProblem)
    }
