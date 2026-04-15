package domains.judger.http

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.judger.application.JudgerRegistryCommands
import judgeprotocol.model.{JudgerHeartbeatRequest, JudgerId, RegisterJudgerRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object JudgerRegistryRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new JudgerRegistryHttpHandlers(databaseSession, judgeConfig)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "internal" / "judgers" / "register" =>
        handlers.register(request)

      case request @ POST -> Root / "api" / "internal" / "judgers" / rawJudgerId / "heartbeat" =>
        handlers.heartbeat(request, rawJudgerId)
    }
