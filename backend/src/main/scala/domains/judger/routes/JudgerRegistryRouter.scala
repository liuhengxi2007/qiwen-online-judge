package domains.judger.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.judge.utils.JudgeConfig
import domains.judger.api.*
import org.http4s.HttpRoutes

object JudgerRegistryRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, sessionStore: SessionStore): HttpRoutes[IO] =
    ApiObjectRouter.routes(
      ApiObjectContext(databaseSession, SessionResolver(sessionStore)),
      List(
        ListRegisteredJudgers(judgeConfig),
        RegisterJudger(judgeConfig),
        RecordJudgerHeartbeat(judgeConfig)
      )
    )
