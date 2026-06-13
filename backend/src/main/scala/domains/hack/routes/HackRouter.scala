package domains.hack.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.hack.api.*
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

/** hack 域路由装配器；注册 hack 查询、创建、worker claim 和结果记录 API。 */
object HackRouter:

  /** 构造 hack 域 HttpRoutes；创建和目标检查需要提交源码存储与题目数据存储。 */
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
        GetSubmissionHackSubtask(submissionProgramStorage, problemDataStorage),
        GetSubmissionHackAvailability(submissionProgramStorage, problemDataStorage),
        CreateHack(submissionProgramStorage, problemDataStorage),
        GetHack,
        ListHacks,
        ClaimNextHackAttempt,
        RecordHackAttemptResult(problemDataStorage)
      )
    )
