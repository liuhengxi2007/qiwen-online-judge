package domains.hack.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.auth.utils.SessionStoreContext
import domains.hack.api.*
import domains.problem.utils.ProblemDataStorageContext
import domains.submission.utils.SubmissionProgramStorageContext
import org.http4s.HttpRoutes

/** hack 域路由装配器；注册 hack 查询、创建、worker claim 和结果记录 API。 */
object HackRouter:

  /** 构造 hack 域 HttpRoutes；创建和目标检查需要提交源码存储与题目数据存储。 */
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
        GetSubmissionHackSubtask(submissionProgramStorage, problemDataStorage),
        GetSubmissionHackAvailability(submissionProgramStorage, problemDataStorage),
        CreateHack(submissionProgramStorage, problemDataStorage),
        GetHack,
        ListHacks,
        ClaimNextHackAttempt,
        RecordHackAttemptResult(problemDataStorage)
      )
    )
