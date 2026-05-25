package domains.submission.http.api

import cats.effect.IO
import domains.submission.http.*
import domains.submission.http.mapper.SubmissionHttpRequestMappers
import domains.submission.http.mapper.SubmissionHttpResponseMappers
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object RejudgeSubmission:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "submissions" / rawSubmissionId / "rejudge" =>
        SubmissionHttpRequestMappers.submissionId(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponseMappers.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.rejudgeSubmission)
    }
