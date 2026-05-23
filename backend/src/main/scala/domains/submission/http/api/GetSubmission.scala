package domains.submission.http.api

import cats.effect.IO
import domains.submission.http.*
import domains.submission.http.response.SubmissionHttpResponses
import domains.submission.model.SubmissionId
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetSubmission:

  def routes(context: SubmissionHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" / rawSubmissionId =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            context.handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.getSubmission)
    }
