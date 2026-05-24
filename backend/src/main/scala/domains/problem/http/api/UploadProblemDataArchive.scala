package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses
import domains.problem.http.utils.ProblemDataUploadHttpSupport



import domains.problem.http.*
import cats.effect.IO
import domains.problem.model.{ProblemSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UploadProblemDataArchive:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "archive" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataUploadHttpSupport.uploadMultipartProblemDataArchive(context.databaseSession, context.sessionStore, context.problemDataStorage, request, parsedProblemSlug)
    }
