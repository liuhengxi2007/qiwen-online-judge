package domains.submission.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.submission.api.*
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, submissionProgramStorage: SubmissionProgramStorage): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListSubmissions,
        CreateSubmission(submissionProgramStorage),
        GetSubmission(submissionProgramStorage),
        DeleteSubmission(submissionProgramStorage),
        RejudgeSubmission(submissionProgramStorage),
        ClaimNextJudgeSubmission,
        GetSubmissionJudgeState,
        UpdateSubmissionJudgeState
      )
    )
