package domains.contest.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.contest.api.{AddProblemToContest, CreateContest, CreateContestSubmission, GetContest, GetContestProblem, ListContestRanklist, ListContestRegistrants, ListContests, RegisterContest, UnregisterContest}
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ContestRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, submissionProgramStorage: SubmissionProgramStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListContests,
        CreateContest,
        GetContest,
        RegisterContest,
        UnregisterContest,
        ListContestRegistrants,
        ListContestRanklist,
        GetContestProblem,
        CreateContestSubmission(submissionProgramStorage),
        AddProblemToContest
      )
    )
