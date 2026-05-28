package domains.judger.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.judge.application.JudgeConfig
import domains.judger.http.api.*
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
