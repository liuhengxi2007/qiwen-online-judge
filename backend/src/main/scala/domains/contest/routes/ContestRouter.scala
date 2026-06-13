package domains.contest.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.auth.utils.SessionStoreContext
import domains.contest.api.*
import domains.problem.utils.ProblemDataStorageContext
import domains.submission.api.{CreateContestSubmission, ListContestSubmissions}
import domains.submission.utils.SubmissionProgramStorageContext
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

/** 汇总比赛 domain 的 http4s 路由，并接入提交和题目数据存储依赖。 */
object ContestRouter:

  /** 构造比赛相关 HTTP 路由，包含比赛 API、赛内题目详情和赛内提交入口。 */
  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStoreContext,
    submissionProgramStorage: SubmissionProgramStorageContext,
    problemDataStorage: ProblemDataStorageContext
  ): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListContests,
        CreateContest,
        GetContest,
        GetContestRatingSnapshot,
        RegisterContest,
        UnregisterContest,
        UpdateContest,
        ListContestRegistrants,
        ListContestRanklist,
        ListContestSubmissions,
        GetContestProblem,
        CreateContestSubmission(submissionProgramStorage, problemDataStorage),
        EvaluateContestProblemAttachWarning,
        AddProblemToContest,
        RemoveProblemFromContest
      )
    )
