package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.Username
import domains.submission.application.SubmissionCommands
import domains.submission.model.{CreateSubmissionRequest, SubmissionId}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          val submitterUsernameFilter = request.uri.query.params.get("username").map(Username.canonical)
          SubmissionCommands
            .listSubmissions(databaseSession, actor, submitterUsernameFilter)
            .flatMap(SubmissionHttpResponses.mapListResult)
        }

      case request @ POST -> Root / "api" / "submissions" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          for
            createRequest <- request.as[CreateSubmissionRequest]
            response <- SubmissionCommands
              .createSubmission(databaseSession, actor, createRequest)
              .flatMap(SubmissionHttpResponses.mapCreateResult)
          yield response
        }

      case request @ GET -> Root / "api" / "submissions" / rawSubmissionId =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              SubmissionCommands
                .getSubmission(databaseSession, actor, submissionId)
                .flatMap(SubmissionHttpResponses.mapGetResult)
            }
    }
