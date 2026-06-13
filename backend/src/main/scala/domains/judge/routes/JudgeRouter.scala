package domains.judge.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.judge.utils.JudgeConfig
import domains.judge.api.*
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

/** judge worker 路由装配器；注册任务领取、数据下载和结果上报 API。 */
object JudgeRouter:

  /** 构造 worker 公开路由；认证由各 API 的 x-judge-token 校验完成。 */
  def routes(
    databaseSession: DatabaseSession,
    judgeConfig: JudgeConfig,
    problemDataStorage: ProblemDataStorage,
    submissionProgramStorage: SubmissionProgramStorage
  ): HttpRoutes[IO] =
    ApiObjectRouter.routes(
      ApiObjectContext.public(databaseSession),
      List(
        ClaimJudgeTask(judgeConfig, problemDataStorage, submissionProgramStorage),
        DownloadJudgeProblemData(judgeConfig, problemDataStorage),
        CompleteJudgeSubmission(judgeConfig),
        CompleteHackAttempt(judgeConfig, problemDataStorage)
      )
    )
