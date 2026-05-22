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
import org.http4s.HttpRoutes

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    ListSubmissions.routes(databaseSession, sessionStore) <+>
      CreateSubmission.routes(databaseSession, sessionStore) <+>
      GetSubmission.routes(databaseSession, sessionStore) <+>
      DeleteSubmission.routes(databaseSession, sessionStore) <+>
      RejudgeSubmission.routes(databaseSession, sessionStore)
