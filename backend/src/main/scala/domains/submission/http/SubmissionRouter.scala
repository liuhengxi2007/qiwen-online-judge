package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.Username
import domains.submission.application.SubmissionCommands
import domains.submission.model.{CreateSubmissionRequest, SubmissionId}
import domains.shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" =>
        handlers.execute(
          request,
          request.uri.query.params.get("username").map(Username.canonical),
          SubmissionHttpPlanDefinitions.listSubmissions
        )

      case request @ POST -> Root / "api" / "submissions" =>
        handlers.executeDecoded[CreateSubmissionRequest, CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult](
          request,
          SubmissionHttpPlanDefinitions.createSubmission
        )(identity)

      case request @ GET -> Root / "api" / "submissions" / rawSubmissionId =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.getSubmission)

      case request @ POST -> Root / "api" / "submissions" / rawSubmissionId / "delete" =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.deleteSubmission)

      case request @ POST -> Root / "api" / "submissions" / rawSubmissionId / "rejudge" =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.rejudgeSubmission)
    }
