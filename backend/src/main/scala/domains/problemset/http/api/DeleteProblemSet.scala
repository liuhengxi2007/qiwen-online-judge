package domains.problemset.http.api

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers
import domains.problemset.http.mapper.ProblemSetHttpRequestMappers



import domains.problemset.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object DeleteProblemSet:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "delete" =>
        ProblemSetHttpRequestMappers.problemSetSlug(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.execute(request, parsedProblemSetSlug, ProblemSetHttpPlanDefinitions.deleteProblemSet)
    }
