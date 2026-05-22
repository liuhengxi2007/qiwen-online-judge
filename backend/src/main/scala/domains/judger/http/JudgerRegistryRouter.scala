package domains.judger.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.judger.http.api.ListRegisteredJudgers
import domains.judger.http.api.RegisterJudger
import domains.judger.http.api.RecordJudgerHeartbeat
import domains.judge.application.JudgeConfig
import org.http4s.HttpRoutes

object JudgerRegistryRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, sessionStore: SessionStore): HttpRoutes[IO] =
    ListRegisteredJudgers.routes(databaseSession, judgeConfig, sessionStore) <+>
      RegisterJudger.routes(databaseSession, judgeConfig, sessionStore) <+>
      RecordJudgerHeartbeat.routes(databaseSession, judgeConfig, sessionStore)
