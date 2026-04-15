package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.Username
import domains.submission.application.SubmissionCommands
import domains.submission.model.{CreateSubmissionRequest, SubmissionId}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class SubmissionHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def listSubmissions(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      val submitterUsernameFilter = request.uri.query.params.get("username").map(Username.canonical)
      SubmissionCommands
        .listSubmissions(databaseSession, actor, submitterUsernameFilter)
        .flatMap(SubmissionHttpResponses.mapListResult)
    }

  def createSubmission(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        createRequest <- request.as[CreateSubmissionRequest]
        response <- SubmissionCommands
          .createSubmission(databaseSession, actor, createRequest)
          .flatMap(SubmissionHttpResponses.mapCreateResult)
      yield response
    }

  def getSubmission(request: Request[IO], submissionId: SubmissionId): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      SubmissionCommands
        .getSubmission(databaseSession, actor, submissionId)
        .flatMap(SubmissionHttpResponses.mapGetResult)
    }
