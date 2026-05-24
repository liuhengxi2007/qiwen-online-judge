package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses



import domains.problem.http.*
import cats.effect.IO
import domains.problem.model.{ProblemDataPath, ProblemSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

private object PathQueryParamMatcher extends org.http4s.dsl.impl.QueryParamDecoderMatcher[String]("path")

object DownloadProblemDataPath:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "api" / "problems" / problemSlug / "data" / "file" :? PathQueryParamMatcher(rawPath) =>
        (ProblemSlug.parse(problemSlug), ProblemDataPath.parse(rawPath)) match
          case (Left(message), _) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSlug), Right(parsedPath)) =>
            ProblemHttpResponses.downloadDataPathResponse(context.problemDataStorage, parsedProblemSlug, parsedPath)
    }
