package domains.judger.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.judge.utils.JudgeConfig
import domains.judger.api.*
import org.http4s.HttpRoutes

/** judger 注册表路由装配器；注册管理端列表、worker 注册/心跳和内部活动语言查询。 */
object JudgerRegistryRouter:

  /** 构造 judger registry 路由；管理端接口走会话，worker 接口走 token。 */
  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, sessionStore: SessionStoreContext): HttpRoutes[IO] =
    ApiObjectRouter.routes(
      ApiObjectContext(databaseSession, sessionStore),
      List(
        ListRegisteredJudgers(judgeConfig),
        RegisterJudger(judgeConfig),
        RecordJudgerHeartbeat(judgeConfig),
        GetActiveJudgerSupportedLanguages
      )
    )
