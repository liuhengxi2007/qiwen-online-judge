package domains.problem.http.api

import domains.problem.http.mapper.ProblemHttpResponseMappers
import domains.problem.http.mapper.ProblemHttpRequestMappers



import domains.problem.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

private object PathQueryParamMatcher extends org.http4s.dsl.impl.QueryParamDecoderMatcher[String]("path")

object DownloadProblemDataPath:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "api" / "problems" / problemSlug / "data" / "file" :? PathQueryParamMatcher(rawPath) =>
        ProblemHttpRequestMappers.problemSlugAndPath(problemSlug, rawPath) match
          case Left(message) =>
            ProblemHttpResponseMappers.validationErrorResponse(message)
          case Right((parsedProblemSlug, parsedPath)) =>
            ProblemHttpResponseMappers.downloadDataPathResponse(context.problemDataStorage, parsedProblemSlug, parsedPath)
    }
