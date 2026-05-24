package domains.judge.http.api

import domains.judge.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.problem.application.ProblemDataStorage
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CompleteJudgeSubmission:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new JudgeHttpHandlers(databaseSession, judgeConfig, problemDataStorage)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "internal" / "judge" / "submissions" / rawSubmissionId / "complete" =>
        handlers.completeSubmission(request, rawSubmissionId)
    }
