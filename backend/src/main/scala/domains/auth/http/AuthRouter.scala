package domains.auth.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.http.api.GetSession
import domains.auth.http.api.Logout
import domains.auth.http.api.ListRegisteredJudgers
import domains.auth.http.api.Login
import domains.auth.http.api.Register
import domains.auth.application.SessionStore
import domains.judge.application.JudgeConfig
import org.http4s.HttpRoutes

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    GetSession.routes(databaseSession, sessionStore, judgeConfig) <+>
      Logout.routes(databaseSession, sessionStore, judgeConfig) <+>
      ListRegisteredJudgers.routes(databaseSession, sessionStore, judgeConfig) <+>
      Login.routes(databaseSession, sessionStore, judgeConfig) <+>
      Register.routes(databaseSession, sessionStore, judgeConfig)
