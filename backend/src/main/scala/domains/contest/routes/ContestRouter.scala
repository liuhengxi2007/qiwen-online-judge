package domains.contest.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.contest.api.*
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.{CreateContestSubmission, ListContestSubmissions}
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ContestRouter:

  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    problemDataStorage: ProblemDataStorage,
    submissionProgramStorage: SubmissionProgramStorage
  ): HttpRoutes[IO] =
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
        UpdateContest,
        ListContestRegistrants,
        ListContestRanklist,
        ListContestSubmissions,
        GetContestProblem,
        CreateContestSubmission(submissionProgramStorage),
        EvaluateContestProblemAttachWarning,
        ListManageableContestProblemSuggestions,
        AddProblemToContest,
        RemoveProblemFromContest,
        UpdateContestProblem,
        DeleteContestProblem(submissionProgramStorage),
        UploadContestProblemDataFile(problemDataStorage),
        ListContestProblemDataTree,
        DownloadContestProblemDataPath(problemDataStorage),
        DeleteContestProblemDataPath(problemDataStorage),
        UploadContestProblemDataArchive(problemDataStorage),
        ClearContestProblemData(problemDataStorage),
        SetContestProblemDataReady(problemDataStorage)
      )
    )
