package domains.judger.http.api



import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.judge.application.JudgeConfig
import domains.judger.http.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListRegisteredJudgers:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new JudgerRegistryHttpHandlers(databaseSession, judgeConfig, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "judgers" =>
        handlers.listRegistered(request)
    }
