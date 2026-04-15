package domains.judge.http

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.{JudgeCommands, JudgeConfig}
import judgeprotocol.model.{ClaimJudgeTaskRequest, ReportJudgeResultRequest}
import domains.submission.model.SubmissionId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object JudgeRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new JudgeHttpHandlers(databaseSession, judgeConfig)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "internal" / "judge" / "claim" =>
        handlers.claim(request)

      case request @ POST -> Root / "api" / "internal" / "judge" / "submissions" / rawSubmissionId / "complete" =>
        handlers.completeSubmission(request, rawSubmissionId)
    }
