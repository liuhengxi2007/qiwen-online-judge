package domains.submission.http.api

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.submission.http.*
import domains.submission.http.response.SubmissionHttpResponses
import domains.submission.model.SubmissionId
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import shared.http.AuthenticatedHttpExecutor

object GetSubmission:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" / rawSubmissionId =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.getSubmission)
    }
