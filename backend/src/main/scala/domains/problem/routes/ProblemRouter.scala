package domains.problem.routes



import cats.effect.IO
import database.DatabaseSession
import domains.problem.api.ListProblems
import domains.problem.api.ListProblemSuggestions
import domains.problem.api.ListManageableProblemSuggestions
import domains.problem.api.CreateProblem
import domains.problem.api.GetProblem
import domains.problem.api.ListProblemDataFiles
import domains.problem.api.ListProblemDataTree
import domains.problem.api.DownloadProblemDataPath
import domains.problem.api.DownloadProblemDataArchive
import domains.problem.api.EvaluateProblemAccess
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.api.ResolveProblemReference
import domains.problem.api.DeleteProblemDataPath
import domains.problem.api.ClearProblemData
import domains.problem.api.SetProblemDataReady
import domains.problem.api.RejudgeProblemSubmissions
import domains.problem.api.UploadProblemDataFile
import domains.problem.api.UploadProblemDataArchive
import domains.problem.api.UpdateProblem
import domains.problem.api.DeleteProblem
import domains.auth.api.SessionStoreContext
import domains.problem.api.ProblemDataStorageContext
import domains.submission.api.SubmissionProgramStorageContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

/** 题目域路由装配器；集中注册公开题目、管理题目数据和内部题目引用相关 API。 */
object ProblemRouter:

  /** 构造题目域 HttpRoutes；注入数据库会话、会话解析器、题目数据存储和提交程序存储。 */
  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStoreContext,
    problemDataStorage: ProblemDataStorageContext,
    submissionProgramStorage: SubmissionProgramStorageContext
  ): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListProblemSuggestions,
        ListManageableProblemSuggestions,
        ListProblems,
        CreateProblem,
        GetProblem,
        UpdateProblem,
        DeleteProblem(submissionProgramStorage),
        ListProblemDataFiles(problemDataStorage),
        ListProblemDataTree,
        DownloadProblemDataPath(problemDataStorage),
        DownloadProblemDataArchive(problemDataStorage),
        DeleteProblemDataPath(problemDataStorage),
        ClearProblemData(problemDataStorage),
        SetProblemDataReady(problemDataStorage),
        RejudgeProblemSubmissions,
        UploadProblemDataFile(problemDataStorage),
        UploadProblemDataArchive(problemDataStorage),
        ResolveProblemReference,
        EvaluateProblemAccess,
        GetJudgeProblemDataManifest
      )
    )
