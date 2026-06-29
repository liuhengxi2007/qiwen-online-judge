package domains.submission.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.problem.api.ProblemDataStorageContext
import domains.submission.api.*
import domains.submission.api.SubmissionProgramStorageContext
import org.http4s.HttpRoutes

/** 提交域路由装配器；注册提交查询、创建、删除、重判和内部判题状态 API。 */
object SubmissionRouter:

  /** 构造提交域 HttpRoutes；注入源码对象存储和题目数据存储以支持创建与详情读取。 */
  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStoreContext,
    submissionProgramStorage: SubmissionProgramStorageContext,
    problemDataStorage: ProblemDataStorageContext
  ): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      context,
      List(
        ListSubmissions,
        CreateSubmission(submissionProgramStorage, problemDataStorage),
        GetSubmission(submissionProgramStorage),
        DeleteSubmission(submissionProgramStorage),
        RejudgeSubmission(submissionProgramStorage),
        ClaimNextJudgeSubmission,
        GetSubmissionJudgeState,
        UpdateSubmissionJudgeState,
        QueueHackRejudgeForProblem,
        QueueManualProblemRejudgeForProblem,
        RequeueStaleRejudgeRevisionSubmission
      )
    )
