package domains.submission.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.*
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

/** 提交域路由装配器；注册提交查询、创建、删除、重判和内部判题状态 API。 */
object SubmissionRouter:

  /** 构造提交域 HttpRoutes；注入源码对象存储和题目数据存储以支持创建与详情读取。 */
  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    submissionProgramStorage: SubmissionProgramStorage,
    problemDataStorage: ProblemDataStorage
  ): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

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
        RequeueStaleHackRevisionSubmission
      )
    )
