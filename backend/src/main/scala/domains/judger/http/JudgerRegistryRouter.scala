package domains.judger.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.judger.http.api.RegisterJudger
import domains.judger.http.api.RecordJudgerHeartbeat
import domains.judge.application.JudgeConfig
import org.http4s.HttpRoutes

object JudgerRegistryRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    RegisterJudger.routes(databaseSession, judgeConfig) <+>
      RecordJudgerHeartbeat.routes(databaseSession, judgeConfig)
