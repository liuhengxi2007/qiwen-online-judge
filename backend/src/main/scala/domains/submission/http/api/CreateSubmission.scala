package domains.submission.http.api

import cats.effect.IO
import domains.submission.application.SubmissionCommands
import domains.submission.model.request.CreateSubmissionRequest
import domains.submission.http.*
import domains.submission.http.codec.SubmissionHttpCodecs.given
import domains.submission.http.mapper.SubmissionHttpRequestMappers
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateSubmission:

  def routes(context: SubmissionHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "submissions" =>
        context.handlers.executeDecoded[CreateSubmissionRequest, CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult](
          request,
          SubmissionHttpPlanDefinitions.createSubmission
        )(SubmissionHttpRequestMappers.createSubmissionRequest)
    }
