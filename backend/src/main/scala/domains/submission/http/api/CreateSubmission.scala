package domains.submission.http.api

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.submission.application.SubmissionCommands
import domains.submission.application.input.CreateSubmissionRequest
import domains.submission.http.*
import domains.submission.http.codec.SubmissionHttpCodecs.given
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import shared.http.AuthenticatedHttpExecutor

object CreateSubmission:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "submissions" =>
        handlers.executeDecoded[CreateSubmissionRequest, CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult](
          request,
          SubmissionHttpPlanDefinitions.createSubmission
        )(identity)
    }
