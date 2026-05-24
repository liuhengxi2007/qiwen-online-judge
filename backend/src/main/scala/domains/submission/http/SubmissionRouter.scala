package domains.submission.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.submission.http.api.ListSubmissions
import domains.submission.http.api.CreateSubmission
import domains.submission.http.api.GetSubmission
import domains.submission.http.api.DeleteSubmission
import domains.submission.http.api.RejudgeSubmission
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = SubmissionHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    )

    ListSubmissions.routes(context) <+>
      CreateSubmission.routes(context) <+>
      GetSubmission.routes(context) <+>
      DeleteSubmission.routes(context) <+>
      RejudgeSubmission.routes(context)
