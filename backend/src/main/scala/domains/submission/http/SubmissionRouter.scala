package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.submission.http.api.*
import org.http4s.HttpRoutes

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListSubmissions,
        CreateSubmission,
        GetSubmission,
        DeleteSubmission,
        RejudgeSubmission
      )
    )
